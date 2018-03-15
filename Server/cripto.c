
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <math.h>

#include "dataThread.h"
#include "sqlQuery.h"
#include "comsock.h"
#include "cripto.h"
#include "errors.h"
#include "sha2.h"

/** alfabeto utilizzato per decriptare un messaggio */
static const char *alfabeto = { "abcdefghijklmnopqrstuvxywzABCDEFGHIJKLMNOPQRSTUVXYWZ0123456789!\"£$%&/()=?^,;.:-_" };
/** dimensione di un blocco dati (dipende dal valore di n) */
static int BLOCK;
/** parametri utilizzati dal cifrario RSA */
static const int P = 131, Q = 359, E = 577;
static int n, phi, d;
/** chiave utilizzata per decriptare il messaggio */
static const char *KEY = 
"1001100101100110110101101001010110100101010110110101010110101010010100100101010101010101010111010101010001011000110010010100010101010010100010101011010000101010101011001010010100000010101010111111001111010101101101010100011010101101010101010010110010011000";
/* lunghezza della chiave */
static int key_length;

/** ottiene il valore in base 10 del numero binario */
#define GET_KEY_FROM_BINARY( binary, i, value )				\
	(value) = 0;											\
	for((i) = (BLOCK - 1); (i) >= 0; (i)--)					\
		if((binary)[i] == '1')								\
			(value) = (value) + pow( 2, (BLOCK - 1) - i );

/** decripta il messaggio con l'RSA */
#define RSA_DECRYPT( i, key, value )	\
	(key) = 1;							\
	for((i) = 0; (i) < d; (i)++)		\
		(key) = ((key) * (value)) % n;

/** dimensione del digest (hash eseguito su una stringa) */
#define DIGEST_SIZE 32

void CRIPTO_init()
{
	unsigned int s;

	n = P * Q;
	phi = (P - 1) * (Q - 1);

	/* calcola il numero di bit di un blocco */
	BLOCK = (int)(log( n ) / log( 2 )) + 2;

	/* ottiene la chiave privata d */
	d = 1;
	while((s = (d * E) % phi) != 1)
		d++;

	key_length = strlen( KEY );
}

/** ottiene l'hash del messaggio attraverso l'uso di sha2
 *
 * \param [in] message - il messaggio su cui eseguire l'hash
 * \param [in] length - lunghezza del messaggio
 *
 * @return 0 se tutto e' andato bene, < 0 altrimenti
*/
static int getSha2( char *message, unsigned int length )
{
	unsigned char digest[DIGEST_SIZE];
	unsigned int i;

	if(message == NULL || length != strlen( message )){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	/* calcola l'hash usando sha2, inserendo il risultato in digest */
	sha2( (unsigned char *) message, length, digest, 0 );

	/* converte i numeri esadecimali in caratteri (ogni numero occupa 2 posizioni) */
	for(i = 0; i < DIGEST_SIZE; i++)
		snprintf( message + (2 * i), N, "%02x", digest[i] );
	message[2 * i] = '\0';

	return 0;
}

int decrypt( char *message, int makeHash )
{
	char tmp;
	int i, j, size, length;
	unsigned int key, value;

	if(message == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	/* inverte il messaggio */
	length = strlen( message );
	for(i = 0; i < length / 2; i++){
		tmp = message[i];
		message[i] = message[length - i - 1];
		message[length - i - 1] = tmp;
	}

	/* ottiene il numero casuale per lo XOR */
	for(i = 0; i < BLOCK; i++){
		if(message[i] == KEY[i])
			message[i] = '0';
		else
			message[i] = '1';
	}

	GET_KEY_FROM_BINARY( message, j, value );
	RSA_DECRYPT( j, key, value );

	length = strlen( message );

	/* effettua lo XOR tra i bit del messaggio e quelli della chiave */
	for(i = BLOCK; i < length; i++){
		if(message[i] == KEY[key])
			message[i] = '0';
		else
			message[i] = '1';

		key = (key + 1) % key_length;
	}

	/* ottiene la lunghezza del messaggio originale */
	GET_KEY_FROM_BINARY( message + BLOCK, j, value );
	RSA_DECRYPT( j, length, value );
	length = length * BLOCK + BLOCK * 2;

	size = 0;
	/* decripta il messaggio */
	for(i = BLOCK * 2; i < length; i = i + BLOCK){
		GET_KEY_FROM_BINARY( (message + i), j, value );

		/* ottiene l'indice della chiave nell'alfabeto */
		RSA_DECRYPT( j, key, value );
		message[size++] = alfabeto[key];
	}

	message[size] = '\0';

	if(makeHash == 1){
		if(getSha2( message, size ) < 0)
			return -1;
	}

	return 0;
}
