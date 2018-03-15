
#ifndef _CRIPTO_H
#define _CRIPTO_H

/** inizializza le strutture dati */
void CRIPTO_init();

/** ottiene il messaggio decriptato
 *
 * \param [in] message - il messaggio da criptare
 * \param [in] makeHash - determina se eseguire l'hash sul messaggio
 *
 * @return 0 se tutto e' andato bene, < 0 altrimenti
*/
int decrypt( char *message, int makeHash );

#endif
