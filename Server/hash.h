
#ifndef _HASH_H
#define _HASH_H

/** restituisce la posizione del nome richiesto
 * @param ID - ID dell'account da cercare
 * @param key - puntatore alla chiave per eseguire la ricerca
 * @param index - puntatore alla prima posizione libera
 *
 * @return -1 se non e' stato trovato, l'indice (>= 0) altrimenti
*/
int HASH_search( unsigned int ID, int *key, int *index );

/** inserisce un nuovo account nella struttura
 * @param ID - identificativo dell'account
 *
 * @return -1 se l'account e' ancora attivo, 0 altrimenti
*/
int HASH_insert( unsigned int ID );

/** rimuove l'account dalla struttura
 * @param ID - identificatore del personaggio
*/
void HASH_remove( unsigned int ID );

#endif
