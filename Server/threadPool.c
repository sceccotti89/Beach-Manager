
#include <stdio.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <assert.h>

#include "threadPool.h"
#include "errors.h"

/* numero di tentativi per avviare un thread */
#define MAX_TRY 5
/* variabile che imposta il delay in secondi da attendere per l'avvio di un thread */
#define DELAY 1

/* mutex per la mutua esclusione sul pool di thread */
static pthread_mutex_t mux_pool = PTHREAD_MUTEX_INITIALIZER;
/* vettore per il passaggio degli indici ai worker */
int num[POOL_SIZE];

int POOL_init()
{
	unsigned int i;

	for(i = 0; i < POOL_SIZE; i++){
		pool[i].fd = -1;
		pool[i].active = 0;
		if(pthread_cond_init( &(pool[i].cond), NULL ) != 0)
			return -1;
	}

	return 0;
}

void waitEndWorkers()
{
	int i, status;

	for(i = 0; i < POOL_SIZE; i++){
		pthread_join( pool[i].tid, (void*) &status );

		if(pool[i].fd != -1){
			fprintf( stderr, "[WORKER] %d: closing socket...\n", i );

			if(closeSocket( pool[i].fd ) == -1)
				fprintf( stderr, "[WORKER] %d: error closing socket\n", i );
			else
				fprintf( stderr, "[WORKER] %d: socket succesfully closed\n", i );
		}
	}
}

int findActivateThread( int fd, char *ip )
{
	int step = 0, value = -1;
	static int lookup = 0;

	if(pthread_mutex_lock( &mux_pool ) != 0)
		return ERROR_MUTEX;

	while(step < POOL_SIZE){
		if(pool[lookup].active == 0){
			pool[lookup].active = 1;

			strncpy( pool[lookup].ip, ip, IPv6 );
			pool[lookup].fd = fd;
			value = lookup;

			if(pthread_cond_signal( &(pool[lookup].cond) ) != 0){
				pthread_mutex_unlock( &mux_pool );
				return ERROR_MUTEX;
			}

			break;
		}
		step++;
		lookup = (lookup + 1) % POOL_SIZE;
	}

	if(pthread_mutex_unlock( &mux_pool ) != 0)
		return ERROR_MUTEX;

	return value;
}

int tryStartThread( pthread_t *t, void *(*routine) (void *), void *arg )
{
	unsigned int count = 1;

	errno = 0;
    while(pthread_create( t, NULL, routine, arg ) != 0 && errno == EAGAIN && count <= MAX_TRY){
    	errno = 0;

		fprintf( stderr, "no enough resource for starting Worker, retry soon...\n" );

		count++;

		sleep( DELAY );
	}

    if(count > MAX_TRY)
    	return -1;

    return 0;
}

int startWorkers( void *(*routine) (void *) )
{
	unsigned int i;

	if(pthread_mutex_lock( &mux_pool ) != 0)
		return -1;

	for(i = 0; i < POOL_SIZE; i++){
		/* si assicura che ci sia lo spazio per rappresentare un intero */
		assert( sizeof( int* ) <= sizeof( void* ));

		num[i] = i;

		if(tryStartThread( &(pool[i].tid), routine, (void *)(num + i) ) == -1){
			pthread_mutex_unlock( &mux_pool );
			return -1;
		}
	}

	if(pthread_mutex_unlock( &mux_pool ) != 0)
		return -1;

	return 0;
}

void closePool()
{
	unsigned int i;

	pthread_mutex_lock( &mux_pool );

	for(i = 0; i< POOL_SIZE; i++){
		if(pool[i].active == 0){
			pool[i].active = 1;

			pthread_cond_signal( &(pool[i].cond) );
		}
		else
			pthread_kill( pool[i].tid, SIGUSR1 );
	}

	pthread_mutex_unlock( &mux_pool );
}

int waitForActivation( int my_id, int *fd )
{
	if(pthread_mutex_lock( &mux_pool ) != 0)
		return -1;

	while(pool[my_id].active == 0){
		if(pthread_cond_wait( &(pool[my_id].cond), &mux_pool ) != 0){
			pthread_mutex_unlock( &mux_pool );
			return -1;
		}
	}

	fprintf( stderr, "[WORKER] %d: ready to work\n", my_id );

	*fd = pool[my_id].fd;

	if(pthread_mutex_unlock( &mux_pool ) != 0)
		return -1;

	return 0;
}

int goingBackToSleep( int my_id )
{
	if(pthread_mutex_lock( &mux_pool ) != 0)
		return -1;

	pool[my_id].active = 0;

	memset( pool[my_id].ip, '\0', IPv6 );

	if(pool[my_id].fd != -1){
		fprintf( stderr, "[WORKER] %d: closing socket...\n", my_id );

		if(closeSocket( pool[my_id].fd ) == -1)
			fprintf( stderr, "[WORKER] %d: error closing socket\n", my_id );

		pool[my_id].fd = -1;
	}

	if(pthread_mutex_unlock( &mux_pool ) != 0)
		return -1;

	return 0;
}
