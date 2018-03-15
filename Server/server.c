
#include <stdio.h>
#include <stdlib.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <assert.h>
#include <mcheck.h>
#include <time.h>

#include "sqlQuery.h"
#include "comsock.h"
#include "errors.h"
#include "dataThread.h"
#include "cripto.h"
#include "threadPool.h"
#include "settings.h"

/* timeout per l'aggiornamento delle prenotazioni */
#define ONE_DAY_TIMER 86400

/* TID del thread dispatcher e signal_handler */
static pthread_t dispatcher, sig_handler;
/* contiene l'ora e il giorno corrente */
static time_t date;
/* flag di chiusura del server */
static int flag_close = 0;

/** il thread per gestire le connessioni
 *
 * \param [in] arg - l'argomento del thread
*/
static void* worker( void *arg )
{
	int ID, fd = -1, index, error;
	char buffer1[N], buffer2[N], query[N];
	bool_t check_version;

	if(arg == NULL){
		errno = EINVAL;
		pthread_exit( (void*) ERROR_EINVAL );
	}

	memset( buffer1, '\0', N );
	memset( buffer2, '\0', N );
	memset( query, '\0', N );

	/* ottiene l'indice nel pool di thread */
	ID = *((int*)arg);

	while(TRUE){
		/* attende di essere attivato */
		if(waitForActivation( ID, &fd ) < 0){
			printf( "[WORKER] %d - terminated with error\n", ID );
			pthread_exit( (void*) -1 );
		}

		if(flag_close == 1)
			break;

		check_version = TRUE;

		/* controlla il login */
		if((index = checkLogin( fd, &check_version, buffer1, buffer2, query )) >= 0){
			/* invia i dati del database */
			if((error = sendDataToUser( index, fd, buffer1, query )) >= 0){
				/* gestione dei comandi dell'utente */
				if((error = checkRequests( index, fd, buffer1, buffer2, query )) < 0){
					/* invia un messaggio di errore */
					if(error != ERROR_SOCKET){
						snprintf( buffer1, N, "%c\n", MSG_ERROR_SERVER );
						sendMessage( fd, buffer1 );
					}
				}

				data[index].ready = 0;
			}
			else{
				if(error != ERROR_SOCKET){
					/* invia un messaggio di errore */
					snprintf( buffer1, N, "%c\n", MSG_ERROR_SERVER );
					sendMessage( fd, buffer1 );
				}
			}

			data[index].active = 0;
		}
		else{
			/* invia un messaggio di errore */
			if(index != INVALID_VERSION && index != ERROR_SOCKET){
				snprintf( buffer2, N, "%c\n", MSG_ERROR_SERVER );
				sendMessage( fd, buffer2 );
			}
		}

		date = time( NULL );
		fprintf( stderr, "Client [ %s ] has disconnected on: %s", pool[ID].ip, asctime( localtime( &date ) ) );

		memset( buffer1, '\0', N );
		memset( buffer2, '\0', N );
		memset( query, '\0', N );

		if(flag_close != 1){
			if(goingBackToSleep( ID ) < 0){
				printf( "[WORKER] %d - terminated with error\n", ID );
				pthread_exit( (void*) -1 );
			}
		}
		else
			break;
	}

	printf( "[WORKER] %d - terminated\n", ID );
	pthread_exit( (void*) 0 );
}

/** gestore (vuoto) per il segnale SIGUSR1
 *
 * \param [in] signum - valore numerico del segnale ricevuto
*/
static void gestore_sigusr1( int signum )
{
	/* elimina lo warning in fase di compilazione */
	((void) signum);
}

/** calcola quanto deve attendere il server per sincronizzarsi con le 2 di notte */
#define SYNCHRONIZE( buffer )									\
				(ONE_DAY_TIMER -								\
				(strtol( (buffer) + 11, NULL, 10 ) * 3600 +		\
				strtol( (buffer) + 14, NULL, 10 ) * 60 +		\
				strtol( (buffer) + 17, NULL, 10 ))) + 7200;

/** gestore dei segnali
 *
 * \param [in] arg - l'argomento del thread (vuoto)
*/
static void* signal_handler( void *arg )
{
	int sig, update;
	char buffer[N];
	sigset_t set;

	((void) arg);

	/* mascheramento segnali: vengono aggiunti gli unici segnali che vogliamo trattare */
	ec_meno1_1( sigemptyset( &set ), "sigemptyset", pthread_exit( (void*) -1 ) );
	ec_meno1_1( sigaddset( &set, SIGALRM ), "sigaddset", pthread_exit( (void*) -1 ) );
	ec_meno1_1( sigaddset( &set, SIGTERM ), "sigaddset", pthread_exit( (void*) -1 ) );
	ec_meno1_1( sigaddset( &set, SIGUSR1 ), "sigaddset", pthread_exit( (void*) -1 ) );
	ec_meno1_1( sigaddset( &set, SIGINT ), "sigaddset", pthread_exit( (void*) -1 ) );
	ec_meno1_1( sigaddset( &set, SIGPIPE ), "sigaddset", pthread_exit( (void*) -1 ) );
	ec_nonzero_1( pthread_sigmask( SIG_BLOCK, &set, NULL ), "pthread_sigmask", pthread_exit( (void*) -1 ) );

	/* calcola quanti secondi rimangono per le 2 di notte */
	date = time( NULL );
	snprintf( buffer, N, "%s", asctime( localtime( &date ) ) );
	update = SYNCHRONIZE( buffer );
	memset( buffer, '\0', N );

	while(TRUE){
		fprintf( stderr, "[SIGNAL_HANDLER]: going to sleep waiting a signal\n" );
		alarm( update );

		ec_nonzero_1( sigwait( &set, &sig ), "sigwait", pthread_exit( (void*) -1 ) );

		if(sig == SIGALRM){
			fprintf( stderr, "[SIGNAL_HANDLER]: updating booking...\n" );
			if(check_booking( buffer ) < 0)
				break;

			update = ONE_DAY_TIMER;
		}
		else
			break;
	}

	fprintf( stderr, "[SIGNAL_HANDLER]: closing all the other threads...\n" );
	alarm( 0 );
	flag_close = 1;

	/* invia un segnale al dispatcher per sbloccarlo dalla accept */
	if(sig != SIGUSR1)
		ec_nonzero_1( pthread_kill( dispatcher, SIGUSR1 ), "pthread_kill", pthread_exit( (void*) -1 ) );

	closePool();

	fprintf( stderr, "[SIGNAL_HANDLER]: successfully terminated\n" );

	pthread_exit( (void*) 0 );
}

/** crea la socket e accetta le connessioni degli utenti
 *
 * \param [in] arg - l'argomento del thread
*/
static void* start_server( void *arg )
{
	int fd_skt, fd_c;
	int result;
	sigset_t set;
	struct sigaction s;
	char ip[IPv6];
	char buffer[N];

	((void) arg);

	/* crea una maschera vuota, ignorando tutti i segnali */
	ec_meno1_1( sigemptyset( &set ), "sigemptyset", pthread_exit( (void*) -1 ) );

	/* attivazione gestore del segnale SIGUSR1 */
	memset( &s, 0, sizeof( s ) );
	s.sa_handler = gestore_sigusr1;
	ec_meno1_1( sigaction( SIGUSR1, &s, NULL ), "sigaction", pthread_exit( (void*) -1 ) );

	/* crea la sokcet su cui attendere le connessioni */
	ec_meno1_1( fd_skt = createServerChannel(), "createServerChannel", pthread_exit( (void*) -1 ) );

	/* aggiorna lo stato del server nel database */
	ec_nonzero_2( SQL_updateServerStatus( TRUE ), "SQL_updateServerStatus", closeSocket( fd_skt ), pthread_exit( (void*) -1 ) )

	date = time( NULL );
	fprintf( stderr, "[DISPATCHER]: server listening on port: %d\n", PORT );
	fprintf( stderr, "[DISPATCHER]: server online on: %s", asctime( localtime( &date ) ) );

	while(TRUE){
		if(flag_close == 0 && (fd_c = acceptConnection( fd_skt, ip, date )) != -1){
			/* cerca un thread libero per essere svegliato */
			result = findActivateThread( fd_c, ip );
			if(result == ERROR_MUTEX){
				/* invia un segnale all'updater per chiude il server */
				pthread_kill( sig_handler, SIGUSR1 );

				break;
			}

			if(result == -1){
				snprintf( buffer, N, "%c\n%c\n", MSG_NO, SERVER_FULL );
				sendMessage( fd_c, buffer );

				closeSocket( fd_c );

				date = time( NULL );
				fprintf( stderr, "Client [ %s ] has disconnected on: %s", ip, asctime( localtime( &date ) ) );
			}
		}
		else{
			if(errno != EINTR)
				perror( "acceptConnection" );

			break;
		}
	}

	fprintf( stderr, "[DISPATCHER]: waiting the termination of the workers...\n" );
	waitEndWorkers();

	/* chiude la socket */
	fprintf( stderr, "[DISPATCHER]: closing connection socket...\n" );
	ec_meno1_1( closeSocket( fd_skt ), "closeSocket", pthread_exit( (void*) -1 ) );
	fprintf( stderr, "[DISPATCHER]: connection socket closed\n" );

	pthread_exit( (void*) 0 );
}

int main( int argc, char *argv[] )
{
	int status;
	sigset_t set;

	mtrace();

	((void) argc);
	((void) argv);

	/* maschera per i segnali: vengono aggiunti soltanto quelli che vogliamo trattare */
	ec_meno1_1( sigemptyset( &set ), "sigemptyset", return -1 );
	ec_meno1_1( sigaddset( &set, SIGINT ), "sigaddset", return -1 );
	ec_meno1_1( sigaddset( &set, SIGTERM ), "sigaddset", return -1 );
	ec_meno1_1( sigaddset( &set, SIGALRM ), "sigaddset", return -1 );
	ec_meno1_1( sigaddset( &set, SIGPIPE ), "sigaddset", return -1 );
	ec_nonzero_1( pthread_sigmask( SIG_SETMASK, &set, NULL ), "pthread_sigmask", return -1 );

	fprintf( stderr, "[MAIN]: version: %.1f\n", VERSION );
	fprintf( stderr, "[MAIN]: max users online: %d\n", POOL_SIZE );

	/* inizializza le struttura dati per la gestione del server */
	ec_meno1_1( DATA_init(), "DATA_init", return -1 );
	ec_meno1_1( POOL_init(), "POOL_init", return -1 );
	CRIPTO_init();

	/* avvio dei worker */
	fprintf( stderr, "[MAIN]: Now starting Workers...\n" );
	ec_meno1_1( startWorkers( worker ), "startWorker", return -1 );
	fprintf( stderr, "[MAIN]: %d workers successfully started\n", POOL_SIZE );

	/* crea il dispatcher */
	fprintf( stderr, "[MAIN]: creating dispatcher thread...\n" );
	ec_nonzero_1( errno = pthread_create( &dispatcher, NULL, &start_server, NULL ), "pthread_create", return -1 );
	fprintf( stderr, "[MAIN]: dispatcher thread successfully created\n" );

	/* creazione del thread per l'aggiornamento del database */
	fprintf( stderr, "[MAIN]: creating signal handler thread...\n" );
	ec_nonzero_1( errno = pthread_create( &sig_handler, NULL, &signal_handler, NULL ), "pthread_create", return -1 );
	fprintf( stderr, "[MAIN]: signal handler thread successfully created\n" );

	/* attende la terminazione dei vari thread... */

	ec_nonzero_1( pthread_join( dispatcher, (void*) &status ), "pthread_join", return -1 );
	fprintf( stderr, "[MAIN]: dispatcher thread successfully terminated\n" );
	if(status == -1){
		pthread_kill( sig_handler, SIGUSR1 );
		waitEndWorkers();
	}

	ec_nonzero_1( pthread_join( sig_handler, (void*) &status ), "pthread_join", return -1 );
	fprintf( stderr, "[MAIN]: signal handler thread successfully terminated\n" );

	date = time( NULL );
	fprintf( stderr, "\n[MAIN]: server is closed at %s", asctime( localtime( &date ) ) );

	ec_nonzero_1( SQL_updateServerStatus( FALSE ), "SQL_uploadServerStatus", return -1 );

	muntrace();

	return 0;
}
