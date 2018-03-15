
#ifndef _COMSOCK_H
#define _COMSOCK_H

#include <time.h>

/* lunghezza massima del buffer in lettura/scrittura della socket */
#define N 512
/* porta usata dal server */
#define PORT 9000
/* numero massimo di byte di un indirizzo IP versione 4 */
#define IPv4 16
/* numero massimo di byte di un indirizzo IP versione 6 */
#define IPv6 40

/**********  TIPI DI MESSAGGIO SCAMBIATI TRA CLIENT E SERVER  **********/

/** account inserito correttamente */
#define MSG_OK					'A'
/** account sbagliato o inesistente */
#define MSG_NO					'B'
/** invia i dati di un ombrellone */
#define MSG_DATA				'C'
/** non c'e' la prenotazione */
#define MSG_NO_BOOKING			'D'
/** inviato lo status di tutti gli ombrelloni */
#define MSG_FINISH				'E'
/** cancella tutti gli ombrelloni */
#define MSG_DELETE_ALL_PLACES	'F'
/** modifica lo stato di un ombrellone */
#define MSG_ADD_BOOKING			'G'
/** modifica lo stato di un ombrellone */
#define MSG_MODIFY_BOOKING		'H'
/** elimina una prenotazione */
#define MSG_DELETE_BOOKING		'I'
/** aggiunge un ombrellone */
#define MSG_ADD_PLACE			'J'
/** modifica un ombrellone */
#define MSG_MODIFY_PLACE		'K'
/** elimina un ombrellone */
#define MSG_DELETE_PLACE		'L'
/** aggiunge una nuova tariffa */
#define MSG_ADD_TARIFF			'M'
/** modifica una tariffa */
#define MSG_MODIFY_TARIFF		'N'
/** elimina una tariffa */
#define MSG_DELETE_TARIFF		'O'
/** modifica il numero di cabine e sdraie */
#define MSG_MODIFY_DATA			'P'
/** errore interno al server */
#define MSG_ERROR_SERVER		'Q'

/**********************************************************************/

/** scrive un messaggio sulla socket
*
* \param [in] sc - file descriptor della socket
* \param [in] msg - struttura che contiene il messaggio da scrivere 
*   
* \retval n il numero di caratteri inviati
* \retval -1 in caso di errore
*/
int sendMessage( int sc, char *msg );

/** legge un messaggio dalla socket
*
* \param [in] sc - file descriptor della socket
* \param [in] msg - struttura che conterra' il messagio letto
*
* \retval lung lunghezza del buffer letto, se OK 
* \retval -1 in caso di errore     
*/
int receiveMessage( int sc, char *msg );

/** crea una socket AF_INET
*
* \retval s il file descriptor della socket
* \retval -1 in caso di errore
*/
int createServerChannel();

/** accetta una connessione da parte di un client
*
* \param [in] s - socket su cui ci mettiamo in attesa di accettare la connessione
* \param [in] ip - struttura contenente l'ip del client
*
* \retval c il descrittore della socket su cui siamo connessi
* \retval -1 in casi di errore
*/
int acceptConnection( int s, char *ip, time_t date );

/** chiude una socket
*
* \param [in] s - file descriptor della socket
*
* \return 0 se tutto ok, -1 in caso di errore
*/
int closeSocket( int s );

#endif
