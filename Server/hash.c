
#include <string.h>

#include "threadPool.h"
#include "dataThread.h"
#include "hash.h"

/** valori utilizzati nelle funzioni hash */
#define P 104729
#define A 853
#define B 1734

/** funzioni hash universali */
#define HASH1( key ) \
	(((((key) % P) % TABLE_SIZE) * A + B) % P) % TABLE_SIZE
#define HASH2( key ) \
	(((((key) % P) % TABLE_SIZE) * B + A) % P) % TABLE_SIZE

int HASH_search( unsigned int ID, int *key, int *index )
{
	int i, h1, h2;

	for(i = 0; i < POOL_SIZE; i++, (*key)++){
		h1 = HASH1( *key );
		h2 = HASH2( *key );

		/* cerca una posizione libera */
		if(*index == -1){
			if(!data[h1].active)
				*index = h1;
			else{
				if(!data[h2].active)
					*index = h2;
			}
		}

		if(data[h1].ID == ID)
			return h1;

		if(data[h2].ID == ID)
			return h2;
	}

	return -1;
}

int HASH_insert( unsigned int ID )
{
	int i, key, h1, h2;
	int index, pos = -1;

	key = ID;
	index = HASH_search( ID, &key, &pos );

	if(index >= 0){
		/* controlla che l'account non sia in uso */
		if(data[index].active)
			return -1;
	}
	else{
		/* controlla se ha trovato una posizione libera */
		if((index = pos) == -1){
			key++;
			for(i = 0; i < POOL_SIZE; i++, key++){
				h1 = HASH1( key );

				if(!data[h1].active){
					index = h1;
					break;
				}

				h2 = HASH2( key );

				if(!data[h2].active){
					index = h2;
					break;
				}
			}
		}

		/* inserisce l'ID dell'account */
		data[index].ID = ID;
	}

	/* rende attivo l'account */
	data[index].active = 1;

	return index;
}

void HASH_remove( unsigned int ID )
{
	int key = ID;
	int index = 0;

	if((index = HASH_search( ID, &key, &index )) >= 0)
		data[index].active = 0;
}
