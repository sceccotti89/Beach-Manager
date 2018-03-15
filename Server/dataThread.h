
#ifndef _DATATHREAD_H
#define _DATATHREAD_H

#include "settings.h"

/** versione utilizzata dal server */
#define VERSION 1.0

typedef enum{ FALSE, TRUE }bool_t;

/** struttura per i dati degli account */
typedef struct dataWorker{
	/** ID associato all'account nel database */
	unsigned int ID;
	/* descrittore della socket */
	unsigned int out;
	/** determina se il thread e' attivo */
	unsigned int active : 1;
	/** determina se puo' ricevere aggiornamenti */
	unsigned int ready : 1;
}dataWorker;

/** contenitore per la gestione degli account */
dataWorker data[TABLE_SIZE];

/** inizializza le strutture dati
 *
 * @return 0 se tutto e' andato bene, -1 altrimenti
*/
int DATA_init( void );

/** trova eventuali prenotazioni scadute e le cancella
 *
 * @param query - buffer per eseguire la query
 *
 * @return 0 se tutto e' andato bene, < 0 altrimenti
*/
int check_booking( char *query );

/** controlla il login dell'utente
 *
 * @param fd - file descriptor della socket dell'utente
 * @param check_version - determina se ricevere la versione dal client
 * @param buffer - vettore in cui inserire i dati
 * @param password - vettore in cui inserire la password
 * @param query - vettore in cui inserire le query da effettuare
 *
 * @return >= 0 se i dati sono corretti
 * @return -1 se si e' verificato un errore
*/
int checkLogin( unsigned int fd, bool_t *check_version, char *buffer, char *password, char *query );

/** invia i dati della spiaggia
 * 
 * @param index - indice dell'account nel vettore
 * @param fd - descrittore della socket
 * @param buffer - vettore in cui inserire i dati
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int sendDataToUser( unsigned int index, unsigned int fd, char *buffer, char *query );

/** invia un messaggio a tutti gli utenti collegati al server escluso il mittente
 *
 * @param index - l'indice del mittente nel vettore degli account (-1 per inviare anche al mittente)
 * @param message - il messaggio da inviare
 *
 * @return 0 se tutto e' andato bene, < 0 altrimenti
*/
int multicast( int index, char *message );

/** sezione di controllo per la gestione della spiaggia
 *
 * @param index - indice nel vettore dei thread
 * @param fd - descrittore della socket
 * @param buffer - vettore per ricevere e inviare i dati
 * @param support - vettore per ricevere i dati
 * @param query - vettore per l'inserimento di una query da eseguire
 *
 * @return 0 se tutto e' andato bene
 * @return -1 se si e' verificato un errore
*/
int checkRequests( int index, int fd, char *buffer, char *support, char *query );

#endif
