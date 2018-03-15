
/***************************************************/

/** errore dovuto a una chiusura della socket */
#define ERROR_SOCKET 			-1
/** versione del gioco non valida */
#define INVALID_VERSION 		-2
/** errore generato dal mutex */
#define ERROR_MUTEX 			-3
/** errore generato dagli argomenti */
#define ERROR_EINVAL 			-4
/** errore nel decriptare un messaggio */
#define ERROR_DECRYPT			-5
/** errore nella connessione al database */
#define ERROR_CONNECTION_DB		-6
/** errore nella formattazione della query */
#define ERROR_QUERY				-7

/** account non esistente */
#define ACCOUNT_NOT_EXIST		'A'
/** account gia' in uso */
#define ACCOUNT_ALREADY_USED	'B'
/** il server e' pieno */
#define SERVER_FULL				'C'
/** versione vecchia dell'applicazione */
#define OLD_VERSION				'D'
/** non ci sono piu' cabine libere */
#define NO_FREE_CABINS			'E'
/** non ci sono piu' sdraie libere */
#define NO_FREE_DECKCHAIRS		'F'
/** il posto selezionato e' gia' prenotato */
#define PLACE_ALREADY_BOOKED	'G'

/***************************************************/

/** controlla NULL: stampa errore ed esegue c */
#define ec_null_1( s, m, c ) \
	if((s) == NULL){ perror( m ); c; }

/** controlla NULL: stampa errore ed esegue c e d */
#define ec_null_2( s, m, c, d ) \
	if((s) == NULL){ perror( m ); c; d; }

/** controlla -1: stampa errore ed esegue c */
#define ec_meno1_1( s, m, c ) \
	if((s) == -1){ perror( m ); c; }

/** controlla -1: stampa errore, esegue c e d */
#define ec_meno1_2( s, m, c, d ) \
	if((s) == -1){ perror( m ); c; d; }

/** controlla -1: stampa errore, esegue c, d ed e */
#define ec_meno1_3( s, m, c, d, e ) \
	if((s) == -1){ perror( m ); c; d; e; }

/** controlla -1: stampa errore, esegue c, d, e ed f */
#define ec_meno1_4( s, m, c, d, e, f ) \
	if((s) == -1){ perror( m ); c; d; e; f; }

/** controlla diverso da 0: stampa errore ed esegue c */
#define ec_nonzero_1( s, m, c ) \
	if((s) != 0){ perror( m ); c; }

/** controlla diverso da 0: stampa errore, esegue c e d */
#define ec_nonzero_2( s, m, c, d ) \
	if((s) != 0){ perror( m ); c; d; }

/** controlla diverso da 0: stampa errore, esegue c, d ed e */
#define ec_nonzero_3( s, m, c, d, e ) \
	if((s) != 0){ perror( m ); c; d; e; }

/** controlla diverso da 0: stampa errore, esegue c, d, e ed f */
#define ec_nonzero_4( s, m, c, d, e, f ) \
	if((s) != 0){ perror( m ); c; d; e; f; }
