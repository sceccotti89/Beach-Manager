
#include <pthread.h>

#include "comsock.h"
#include "settings.h"

/** struttura utilizzata per gestire il pool di thread */
typedef struct worker_t{
	/** TID del relativo thread */
	pthread_t tid;
	/** descrittore della socket presso il worker */
	int fd;
	/** ip del client associato al thread */
	char ip[IPv6];
	/** bit che indica se il worker e' attivo */
	unsigned int active : 1;
	/** variabile per l'attesa e il risveglio del worker */
	pthread_cond_t cond;
}worker_t;

/* struttura che contiene tutti i dati degli worker */
worker_t pool[POOL_SIZE];

/** inizializza il pool di thread
 *
 * @return 0 se tutto e' andato bene, -1 altrimenti
*/
int POOL_init();

/** attende la terminazione di tutti gli worker*/
void waitEndWorkers();

/** cerca ed attiva un thread dormiente fra quelli del pool
 *
 * \param [in] fd - il file descriptor della socket da assegnare al nuovo worker
 *
 * \retval -1 se non si e' trovato un thread disponibile
 * \retval l'indice nel pool di thread del thread trovato
 * \retval < 0 in caso di errore
 */
int findActivateThread( int fd, char *ip );

/** prova ad avviare un thread.
 * 	Nel caso in cui non vi siano sufficienti risorse attende per DELAY secondi e prova di nuovo
 * 	ad avviarlo per MAX_TRY volte
 *
 * \param [in, out] t - il TID del thread avviato
 * \param [in] routine - la funzione che indica il thread da avviare
 * \param [in] arg - il parametro della funzione
 *
 * \retval 0 se il thread e' stato avviato correttamente, -1 altrimenti
 */
int tryStartThread( pthread_t *t, void *(*routine) (void *), void *arg );

/** avvia tutti gli worker del pool di thread
 *
 *	\retval -1 se non si e' riusciti ad avviare un thread
 *	\retval 0 negli altri casi
*/
int startWorkers( void *(*routine) (void *) );

/** chiude il pool di thread */
void closePool();

/** il thread si sospende in attesa di essere attivato
 *
 * \param [in] my_id - l'indice nel pool di thread del worker
 * \param [out] fd - conterra' il valore del socket in entrata assegnato al worker
 *
 * \retval 0 se tutto e' andato bene, -1 altrimenti
*/
int waitForActivation( int my_id, int *fd );

/** riporta un worker in fase di attesa, chiude le socket aperte e riporta i valori dei file descriptor al loro valore di default (-1)
 *
 * \param [in] my_id - l'indice nel pool di thread del worker
 *
 * \retval 0 se tutto e' andato bene, -1 altrimenti
*/
int goingBackToSleep( int my_id );
