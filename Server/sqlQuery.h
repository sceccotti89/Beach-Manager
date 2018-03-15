
#ifndef _SQL_QUERY_H
#define _SQL_QUERY_H

/** tipi di query */
#define SQL_NEW 0
#define SQL_UPDATE 1
#define SQL_DELETE 2

/** recupera i dati della spiaggia
 *
 * @param cabins - puntatore al numero di cabine
 * @param price_cabin - prezzo di una cabina
 * @param deckchairs - puntatore al numero di sdraie
 * @param price_deckchairs - prezzo di una sdraia
 *
 * @return 0 se tutto ok, -1 altrimenti
*/
int SQL_getData( unsigned int *cabins, float *price_cabin, unsigned int *deckchairs, float *price_deckchair );

/** controlla se un utente e' registrato
 *
 * @param account - l'account del client
 * @param password - la password del client
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 1 se i dati sono corretti, 0 altrimenti
 * @return -1 se si e' verificato un errore
*/
int SQL_isRegisted( const char *account, const char *password, char *query );

/** trova eventuali prenotazioni scadute e le cancella
 *
 * @param query - buffer per eseguire la query
 *
 * @return 0 se tutto e' andato bene, < 0 altrimenti
*/
int SQL_check_booking( char *query );

/** invia i dati del database all'utente
 *
 * @param index - indice dell'account nel vettore
 * @param fd - descrittore della socket
 * @param buffer - vettore in cui inserire i dati
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int SQL_sendDataToUser( unsigned int index, unsigned int fd, char *buffer, char *query );

/** aggiorna la prenotazione di un ombrellone
 *
 * @param type - tipo di query
 * @param IDPrenotazione - puntatore all'ID della prenotazione
 * @param IDPlace - ID dell'ombrellone
 * @param date_from - data di inizio prenotazione
 * @param date_to - data di fine prenotazione
 * @param name - nome dell'utente
 * @param surname - cognome dell'utente
 * @param phone - numero di telefono dell'utente
 * @param old_cabins - numero delle precedenti cabine prenotate
 * @param cabins - numero di cabine prenotate
 * @param old_deckchairs - numero delle precedenti sdraie prenotate
 * @param deckchairs - numero di sdraie prenotate
 * @param buffer - area contenentela lista degli ID delle cabine 
 * @param query - buffer in cui inserire il risultato della query
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int SQL_update_booking( unsigned int type, unsigned int *IDPrenotazione, unsigned int IDPlace, char *date_from, char *date_to,
						char *name, char *surname, char *phone, unsigned int old_cabins, unsigned int cabins, unsigned int old_deckchairs, unsigned int deckchairs,
						char *error, char *buffer, char *query );

/** aggiunge un posto
 *
 * @param IDPlace - puntatore all'ID del posto appena inserito
 * @param X - coordinata X del luogo da inserire
 * @param Y - coordinata Y del luogo da inserire
 * @param name - nome dell'ombrellone
 * @param price - prezzo della prenotazione
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, -1 altrimenti
*/
int SQL_add_place( unsigned int *IDPlace, unsigned int X, unsigned int Y, char *name, float price, char *query );

/** aggiorna le statistiche di un ombrellone
 *
 * @param IDPlace - ID dell'ombrellone
 * @param name - nome dell'ombrellone
 * @param price - prezzo dell'ombrellone
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int SQL_modify_place( unsigned int IDPlace, char *name, float price, char *query );

/** cancella un posto
 *
 * @param IDPlace - ID del posto da cancellare
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, -1 altrimenti
*/
int SQL_delete_place( unsigned int IDPlace, char *query );

/** cancella tutti gli ombrelloni
 *
 * @return 0 se tutto ok, < 0 altrimenti
*/
int SQL_delete_all();

/** aggiorna una tariffa
 *
 * @param type - tipo di query
 * @param IDPacchetto - puntatore all'ID della tariffa
 * @param date_from - data di inizio prenotazione
 * @param date_to - data di fine prenotazione
 * @param weekly_price - prezzo mensile
 * @param daily_price - prezzo giornaliero
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int SQL_update_tariff( unsigned int type, unsigned int *IDPacchetto, char *date_from, char *date_to, float daily_price, float weekly_price, char *query );

/** modifica il numero di cabine o sdraie
 *
 * @param cabins - numero di cabine
 * @param old_cabins - puntatore al numero delle vecchie cabine
 * @param price_cabin - prezzo di una cabina
 * @param old_price_cabin - puntatore al vecchio numero di cabine
 * @param deckchairs - numero di sdraie
 * @param old_deckchairs - puntatore al numero delle vecchie sdraie
 * @param price_deckchair - prezzo di una sdraia
 * @param old_price_deckchair - puntatore al vecchio numero di sdraie
 * @param query - buffer in cui inserire la query da effettuare
 *
 * @return 0 se e' tutto ok, < 0 altrimenti
*/
int SQL_modify_data( unsigned int cabins, unsigned int *old_cabins, float price_cabin, float *old_price_cabin,
					 unsigned int deckchairs, unsigned int *old_deckchairs, float price_deckchair, float *old_price_deckchair, char *query );

/** aggiorna lo stato del server
 *
 * @param online - determina se il server e' online
 *
 * @return 0 se tutto ok, -1 altrimenti
*/
int SQL_updateServerStatus( int online );

#endif
