
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <time.h>
#include <signal.h>
#include <errno.h>

#include "sqlQuery.h"
#include "errors.h"
#include "dataThread.h"
#include "threadPool.h"
#include "hash.h"
#include "cripto.h"

/** mutex per la gestione esclusiva dei dati */
static pthread_mutex_t SQL_booking_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t SQL_place_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t SQL_tariff_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t data_t = PTHREAD_MUTEX_INITIALIZER;
/** numero di cabine e di sdraie */
static unsigned int cabins = 0, deckchairs = 0;
/** prezzo di una cabina e di una sdraia */
static float prezzo_cabina, prezzo_sdraia;

int DATA_init( void )
{
	unsigned int i;
	char buffer[N];

	/* inizializza i thread */
	for(i = 0; i < TABLE_SIZE; i++){
		data[i].ready = 0;
		data[i].active = 0;
		data[i].ID = 0;
		data[i].out = 0;
	}

	if(SQL_getData( &cabins, &prezzo_cabina, &deckchairs, &prezzo_sdraia ) < 0)
		return -1;

	if(SQL_check_booking( buffer ) < 0)
		return -1;

	return 0;
}

/** attiva tutti i mutex */
#define LOCK_ALL()									\
	pthread_mutex_lock( &SQL_booking_mutex ) ||		\
	pthread_mutex_lock( &SQL_place_mutex )   ||		\
	pthread_mutex_lock( &SQL_tariff_mutex )

/** chiude un mutex */
#define CLOSE( mutex )					\
	pthread_mutex_unlock( &(mutex) );

/** chiude tutti i mutex e la comunicazione al database */
#define CLOSE_ALL()									\
	pthread_mutex_unlock( &SQL_booking_mutex );		\
	pthread_mutex_unlock( &SQL_place_mutex );		\
	pthread_mutex_unlock( &SQL_tariff_mutex );

/** disattiva tutti i mutex */
#define UNLOCK_ALL()								\
	pthread_mutex_unlock( &SQL_booking_mutex );		\
	pthread_mutex_unlock( &SQL_place_mutex );   	\
	pthread_mutex_unlock( &SQL_tariff_mutex );

/** tenta di disattivare tutti i mutex */
#define TRY_UNLOCK_ALL()							\
	pthread_mutex_unlock( &SQL_booking_mutex ) ||	\
	pthread_mutex_unlock( &SQL_place_mutex )   ||	\
	pthread_mutex_unlock( &SQL_tariff_mutex )

int check_booking( char *query )
{
	int error;

	ec_meno1_2( LOCK_ALL(), "pthread_mutex_lock", CLOSE_ALL(), return ERROR_MUTEX );

	if((error = SQL_check_booking( query )) < 0){
		UNLOCK_ALL();
		return error;
	}

	ec_meno1_1( TRY_UNLOCK_ALL(), "pthread_mutex_unlock", return ERROR_MUTEX );

	return 0;
}

int checkLogin( unsigned int fd, bool_t *check_version, char *buffer, char *password, char *query )
{
	unsigned int ID;
	int index;

	if(buffer == NULL || password == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	while(TRUE){
		if(*check_version){
			/* ottiene la versione */
			if(receiveMessage( fd, buffer ) <= 0)
				return ERROR_SOCKET;

			*check_version = FALSE;

			/* controlla che la versione sia corretta, altrimenti chiude la sessione */
			if(strtod( buffer, NULL ) != VERSION){
				snprintf( buffer, N, "%c\n%c\n", MSG_NO, OLD_VERSION );
				sendMessage( fd, buffer );

				return INVALID_VERSION;
			}
		}

		/* ottiene account e password */
		if(receiveMessage( fd, buffer ) <= 0 || receiveMessage( fd, password ) <= 0)
			return ERROR_SOCKET;

		if(decrypt( buffer, FALSE ) < 0 || decrypt( password, TRUE ) < 0)
			return ERROR_DECRYPT;

		/* controlla nel database se l'account e' valido */
		if((ID = SQL_isRegisted( buffer, password, query )) == 0){
			snprintf( buffer, N, "%c\n%c\n", MSG_NO, ACCOUNT_NOT_EXIST );
			sendMessage( fd, buffer );

			continue;
		}

		/* inserisce l'account usando una tabella hash */
		ec_meno1_1( pthread_mutex_lock( &data_t ), "pthread_mutex_lock", return ERROR_MUTEX );

		index = HASH_insert( ID );

		if(index >= 0)
			data[index].out = fd;

		ec_meno1_1( pthread_mutex_unlock( &data_t ), "pthread_mutex_unlock", return ERROR_MUTEX );

		if(index == -1){
			snprintf( buffer, N, "%c\n%c\n", MSG_NO, ACCOUNT_ALREADY_USED );
			sendMessage( fd, buffer );

			continue;
		}

		snprintf( buffer, N, "%c\n%d\n%f\n%d\n%f\n", MSG_OK, cabins, prezzo_cabina, deckchairs, prezzo_sdraia );
		sendMessage( fd, buffer );

		return index;
	}
}

int sendDataToUser( unsigned int index, unsigned int fd, char *buffer, char *query )
{
	int error;

	ec_meno1_2( LOCK_ALL(), "pthread_mutex_lock", CLOSE_ALL(), return ERROR_MUTEX );

	if((error = SQL_sendDataToUser( index, fd, buffer, query )) < 0){
		UNLOCK_ALL();
		return error;
	}

	ec_meno1_1( TRY_UNLOCK_ALL(), "pthread_mutex_unlock", return ERROR_MUTEX );

	return 0;
}

int multicast( int index, char *message )
{
	int i;

	if(message == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	for(i = 0; i < TABLE_SIZE; i++){
		if(index == i || data[i].ready == FALSE)
			continue;

		sendMessage( data[i].out, message );
	}

	return 0;
}

int checkRequests( int index, int fd, char *buffer1, char *buffer2, char *query )
{
	unsigned int IDPlace, IDRate, IDPrenotazione;
	unsigned int X, Y;
	float daily_price, weekly_price, price_cabin, price_deckchair, price;
	unsigned int old_cabins, cabine, old_sdraie, sdraie;
	int result;
	char date_from[N], date_to[N];
	char name[N], surname[N], phone[N];
	char type, error;

	if(fd == -1 || buffer1 == NULL || buffer2 == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	memset( date_from, '\0', N );
	memset( date_to, '\0', N );

	while(TRUE){
		/* legge il tipo di messaggio */
		if(receiveMessage( fd, buffer1 ) <= 0)
			return ERROR_SOCKET;

		type = buffer1[0];
		printf("RICEVUTO: %c\n", type );
		switch( type ){
			case( MSG_ADD_BOOKING ):
				/* ottiene l'ID del posto da modificare */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDPlace = strtol( buffer1, NULL, 0 );

				/* ottiene la data di inizio e fine prenotazione */
				if(receiveMessage( fd, date_from ) <= 0 || receiveMessage( fd, date_to ) <= 0)
					return ERROR_SOCKET;

				/* ottiene nome, cognome e telefono della persona associata */
				if(receiveMessage( fd, name ) <= 0 || receiveMessage( fd, surname ) <= 0 || receiveMessage( fd, phone ) <= 0)
					return ERROR_SOCKET;

				/* il vecchio e il nuovo numero di sdraie */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				cabine = strtol( buffer1, NULL, 0 );
				sdraie = strtol( buffer2, NULL, 0 );

				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_booking( SQL_NEW, &IDPrenotazione, IDPlace, date_from, date_to, name, surname, phone, 0, cabine, 0, sdraie, &error, buffer1, query )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					return result;
				}

				/* avverte l'utente dell'esito della query */
				if(result == 0){
					ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

					snprintf( buffer1, N, "%c\n%c\n", MSG_NO, error );
					sendMessage( fd, buffer1 );
				}
				else{
					/* avverte gli altri utenti */
					if(cabine == 0)
						snprintf( buffer2, N, "%c\n%d\n%d\n%s\n%s\n%s\n%s\n%s\n0\n%d\n",
									MSG_ADD_BOOKING, IDPlace, IDPrenotazione, date_from, date_to, name, surname, phone, sdraie );
					else
						snprintf( buffer2, N, "%c\n%d\n%d\n%s\n%s\n%s\n%s\n%s\n%d\n%d\n%s",
									MSG_ADD_BOOKING, IDPlace, IDPrenotazione, date_from, date_to, name, surname, phone, cabine, sdraie, buffer1 );

					if((result = multicast( index, buffer2 )) < 0){
						pthread_mutex_unlock( &SQL_booking_mutex );
						return result;
					}

					if(cabine == 0)
						snprintf( buffer2, N, "%c\n%d\n0\n", MSG_OK, IDPrenotazione );
					else
						snprintf( buffer2, N, "%c\n%d\n%d\n%s", MSG_OK, IDPrenotazione, cabine, buffer1 );
					sendMessage( fd, buffer2 );

					ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );
				}

				break;

			case( MSG_MODIFY_BOOKING ):
				/* ottiene l'ID del posto */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDPlace = strtol( buffer1, NULL, 0 );

				/* ottiene l'ID della prenotazione */
				if(receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				IDPrenotazione = strtol( buffer2, NULL, 0 );

				/* ottiene la nuova data di inizio e fine prenotazione */
				if(receiveMessage( fd, date_from ) <= 0 || receiveMessage( fd, date_to ) <= 0)
					return ERROR_SOCKET;

				/* ottiene nome, cognome e telefono della persona associata */
				if(receiveMessage( fd, name ) <= 0 || receiveMessage( fd, surname ) <= 0 || receiveMessage( fd, phone ) <= 0)
					return ERROR_SOCKET;

				/* ottiene il vecchio e il nuovo numero di cabine */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				old_cabins = strtol( buffer1, NULL, 0 );
				cabine = strtol( buffer2, NULL, 0 );

				/* ottiene il vecchio e il nuovo numero di sdraie */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				old_sdraie = strtol( buffer1, NULL, 0 );
				sdraie = strtol( buffer2, NULL, 0 );

				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_booking( SQL_UPDATE, &IDPrenotazione, 0, date_from, date_to, name, surname, phone, old_cabins, cabine, old_sdraie, sdraie, &error, buffer1, query )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					return result;
				}

				/* avverte l'utente dell'esito della query */
				if(result == 0){
					ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

					snprintf( buffer1, N, "%c\n%c\n", MSG_NO, error );
					sendMessage( fd, buffer1 );
				}
				else{
					/* avverte gli altri utenti */
					if(cabine > old_cabins)
						snprintf( buffer2, N, "%c\n%d\n%d\n%s\n%s\n%s\n%s\n%s\n%d\n%d\n%d\n%s",
									MSG_MODIFY_BOOKING, IDPlace, IDPrenotazione, date_from, date_to, name, surname, phone, old_cabins, cabine, sdraie, buffer1 );
					else
						snprintf( buffer2, N, "%c\n%d\n%d\n%s\n%s\n%s\n%s\n%s\n%d\n%d\n%d\n",
									MSG_MODIFY_BOOKING, IDPlace, IDPrenotazione, date_from, date_to, name, surname, phone, old_cabins, cabine, sdraie );

					if((result = multicast( index, buffer2 )) < 0){
						pthread_mutex_unlock( &SQL_booking_mutex );
						return result;
					}

					if(cabine > old_cabins)
						snprintf( buffer2, N, "%c\n%d\n%d\n%s", MSG_OK, old_cabins, cabine, buffer1 );
					else
						snprintf( buffer2, N, "%c\n%d\n%d\n", MSG_OK, old_cabins, cabine );
					sendMessage( fd, buffer2 );

					ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );
				}

				break;

			case( MSG_DELETE_BOOKING ):
				/* ottiene l'ID del posto da modificare */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDPlace = strtol( buffer1, NULL, 0 );

				/* ottiene l'ID della prenotazione da modificare */
				if(receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				IDPrenotazione = strtol( buffer2, NULL, 0 );

				/* ottiene il numero di cabine e sdraie */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				cabine = strtol( buffer1, NULL, 0 );
				sdraie = strtol( buffer2, NULL, 0 );

				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_booking( SQL_DELETE, &IDPrenotazione, 0, NULL, NULL, NULL, NULL, NULL, 0, cabine, 0, sdraie, NULL, buffer1, query )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					return result;
				}

				snprintf( buffer2, N, "%c\n%d\n%d\n", MSG_DELETE_BOOKING, IDPlace, IDPrenotazione );
				if((result = multicast( index, buffer2 )) < 0){
					return result;
				}

				ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

				break;

			case( MSG_ADD_PLACE ):
				/* ottiene la coordinata X del posto da aggiungere */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				X = strtol( buffer1, NULL, 0 );

				/* ottiene la coordinata Y del posto da aggiungere */
				if(receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				Y = strtol( buffer2, NULL, 0 );

				/* ottiene il nome del posto da aggiungere */
				if(receiveMessage( fd, name ) <= 0)
					return ERROR_SOCKET;

				/* ottiene la coordinata Y del posto da aggiungere */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				price = strtof( buffer1, NULL );

				ec_meno1_1( pthread_mutex_lock( &SQL_place_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_add_place( &IDPlace, X, Y, name, price, query )) < 0){
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				snprintf( buffer2, N, "%c\n%d\n%d\n%d\n%s\n%f\n", MSG_ADD_PLACE, IDPlace, Y, X, name, price );
				/* invia la notifica anche al client */
				if((result = multicast( -1, buffer2 )) < 0){
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				ec_meno1_1( pthread_mutex_unlock( &SQL_place_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

				break;

			case( MSG_MODIFY_PLACE ):
				/* ottiene l'ID del posto da modificare */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDPlace = strtol( buffer1, NULL, 0 );

				/* ottiene il nome del posto da modificare */
				if(receiveMessage( fd, name ) <= 0)
					return ERROR_SOCKET;

				/* ottiene il costo del posto da modificare */
				if(receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				price = strtof( buffer2, NULL );

				ec_meno1_1( pthread_mutex_lock( &SQL_place_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_modify_place( IDPlace, name, price, query )) < 0){
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				snprintf( buffer1, N, "%c\n%d\n%s\n%f\n", MSG_MODIFY_PLACE, IDPlace, name, price );
				if((result = multicast( index, buffer1 )) < 0){
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				ec_meno1_1( pthread_mutex_unlock( &SQL_place_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

				break;

			case( MSG_DELETE_PLACE ):
				/* ottiene l'ID del posto da eliminare */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDPlace = strtol( buffer1, NULL, 0 );

				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_lock1", return ERROR_MUTEX );
				ec_meno1_2( pthread_mutex_lock( &SQL_place_mutex ), "pthread_mutex_lock2", pthread_mutex_unlock( &SQL_booking_mutex ), return ERROR_MUTEX );

				if((result = SQL_delete_place( IDPlace, query )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				snprintf( buffer1, N, "%c\n%d\n", MSG_DELETE_PLACE, IDPlace );
				if((result = multicast( index, buffer1 )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				ec_meno1_2( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock1", pthread_mutex_unlock( &SQL_place_mutex ), return ERROR_MUTEX );
				ec_meno1_1( pthread_mutex_unlock( &SQL_place_mutex ), "pthread_mutex_unlock2", return ERROR_MUTEX );

				break;

			case( MSG_DELETE_ALL_PLACES ):
				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_lock1", return ERROR_MUTEX );
				ec_meno1_2( pthread_mutex_lock( &SQL_place_mutex ), "pthread_mutex_lock2", pthread_mutex_unlock( &SQL_booking_mutex ), return ERROR_MUTEX );

				if((result = SQL_delete_all()) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				strncat( buffer2, "\n", 1 );
				if((result = multicast( index, buffer2 )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					pthread_mutex_unlock( &SQL_place_mutex );
					return result;
				}

				ec_meno1_2( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_unlock1", pthread_mutex_unlock( &SQL_place_mutex ), return ERROR_MUTEX );
				ec_meno1_1( pthread_mutex_unlock( &SQL_place_mutex ), "pthread_mutex_unlock2", return ERROR_MUTEX );

				break;

			case( MSG_ADD_TARIFF ):
				/* ottiene la nuova data di inizio e fine prenotazione */
				if(receiveMessage( fd, date_from ) <= 0 || receiveMessage( fd, date_to ) <= 0)
					return ERROR_SOCKET;

				/* ottiene il prezzo giornaliero e mensile */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				daily_price = strtof( buffer1, NULL );
				weekly_price = strtof( buffer2, NULL );

				ec_meno1_1( pthread_mutex_lock( &SQL_tariff_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_tariff( SQL_NEW, &IDRate, date_from, date_to, daily_price, weekly_price, query )) < 0){
					pthread_mutex_unlock( &SQL_tariff_mutex );
					return result;
				}

				/* avverte l'utente dell'esito della query */
				if(result == 0){
					ec_meno1_1( pthread_mutex_unlock( &SQL_tariff_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

					snprintf( buffer2, N, "%c\n", MSG_NO );
					sendMessage( fd, buffer2 );
				}
				else{
					/* avverte gli altri utenti */
					snprintf( buffer2, N, "%c\n%d\n%s\n%s\n%f\n%f\n", MSG_ADD_TARIFF, IDRate, date_from, date_to, daily_price, weekly_price );
					if((result = multicast( index, buffer2 )) < 0){
						pthread_mutex_unlock( &SQL_tariff_mutex );
						return result;
					}

					snprintf( buffer1, N, "%c\n%d\n", MSG_OK, IDRate );
					sendMessage( fd, buffer1 );

					ec_meno1_1( pthread_mutex_unlock( &SQL_tariff_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );
				}

				break;

			case( MSG_MODIFY_TARIFF ):
				/* ottiene l'ID della tariffa da modificare */
				if(receiveMessage( fd, buffer1 ) <= 0)
					return ERROR_SOCKET;
				IDRate = strtol( buffer1, NULL, 0 );

				/* ottiene la nuova data di inizio e fine prenotazione */
				if(receiveMessage( fd, date_from ) <= 0 || receiveMessage( fd, date_to ) <= 0)
					return ERROR_SOCKET;

				/* ottiene il prezzo giornaliero e mensile */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				daily_price = strtof( buffer1, NULL );
				weekly_price = strtof( buffer2, NULL );

				ec_meno1_1( pthread_mutex_lock( &SQL_tariff_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_tariff( SQL_UPDATE, &IDRate, date_from, date_to, daily_price, weekly_price, query )) < 0){
					pthread_mutex_unlock( &SQL_tariff_mutex );
					return result;
				}

				/* avverte l'utente dell'esito della query */
				if(result == 0){
					ec_meno1_1( pthread_mutex_unlock( &SQL_tariff_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

					snprintf( buffer2, N, "%c\n", MSG_NO );
					sendMessage( fd, buffer2 );
				}
				else{
					/* avverte gli altri utenti */
					snprintf( buffer1, N, "%c\n%d\n%s\n%s\n%f\n%f\n", MSG_MODIFY_TARIFF, IDRate, date_from, date_to, daily_price, weekly_price );
					if((result = multicast( index, buffer1 )) < 0){
						pthread_mutex_unlock( &SQL_tariff_mutex );
						return result;
					}

					snprintf( buffer2, N, "%c\n", MSG_OK );
					sendMessage( fd, buffer2 );

					ec_meno1_1( pthread_mutex_unlock( &SQL_tariff_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );
				}

				break;

			case( MSG_DELETE_TARIFF ):
				/* ottiene l'ID della tariffa da cancellare */
				if(receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				IDRate = strtol( buffer2, NULL, 0 );

				ec_meno1_1( pthread_mutex_lock( &SQL_tariff_mutex ), "pthread_mutex_lock", return ERROR_MUTEX );

				if((result = SQL_update_tariff( SQL_DELETE, &IDRate, NULL, NULL, 0, 0, query )) < 0){
					pthread_mutex_unlock( &SQL_tariff_mutex );
					return result;
				}

				snprintf( buffer1, N, "%c\n%d\n", MSG_DELETE_TARIFF, IDRate );
				if((result = multicast( index, buffer1 )) < 0){
					pthread_mutex_unlock( &SQL_tariff_mutex );
					return result;
				}

				ec_meno1_1( pthread_mutex_unlock( &SQL_tariff_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

				break;

			case( MSG_MODIFY_DATA ):
				/* ottiene il numero di cabine e sdraie */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				cabine = strtol( buffer1, NULL, 0 );
				price_cabin = strtof( buffer2, NULL );

				/* ottiene il numero di cabine e sdraie */
				if(receiveMessage( fd, buffer1 ) <= 0 || receiveMessage( fd, buffer2 ) <= 0)
					return ERROR_SOCKET;
				sdraie = strtol( buffer1, NULL, 0 );
				price_deckchair = strtof( buffer2, NULL );

				ec_meno1_1( pthread_mutex_lock( &SQL_booking_mutex ), "pthread_mutex_unlock", return ERROR_MUTEX );

				if((result = SQL_modify_data( cabine, &cabins, price_cabin, &prezzo_cabina, sdraie, &deckchairs, price_deckchair, &prezzo_sdraia, query )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					return result;
				}

				snprintf( buffer2, N, "%c\n%d\n%f\n%d\n%f\n", MSG_MODIFY_DATA, cabins, price_cabin, deckchairs, price_deckchair );
				if((result = multicast( index, buffer2 )) < 0){
					pthread_mutex_unlock( &SQL_booking_mutex );
					return result;
				}

				ec_meno1_1( pthread_mutex_unlock( &SQL_booking_mutex ), "pthread_mutex_ununlock", return ERROR_MUTEX );

				break;

			case( MSG_FINISH ):
				/* invia al client la notifica che ha ricevuto e rimanda indietro */
				strncat( buffer1, "\n", 1 );
				sendMessage( fd, buffer1 );

				break;
		}
	}

	return 0;
}
