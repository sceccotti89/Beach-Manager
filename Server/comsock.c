
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/un.h>
#include <fcntl.h>
#include <errno.h>
#include <time.h>

#include "comsock.h"

/* massima lunghezza, in cifre, del messaggio (512 byte per la scrittura atomica) */
#define LENGTHMSG 3

int sendMessage( int sc, char *msg )
{
	int n;

	if((n = write( sc, msg, strlen( msg ) )) <= 0){
		if(n == 0)
			errno = ENOTCONN;

		return -1;
	}

	return n;
}

int receiveMessage( int sc, char *msg ) 
{
	unsigned int length, n;
	int size;

	if(sc == -1 || msg == NULL){
		errno = EINVAL;
		return -1;
	}

	/* legge la lunghezza del prossimo messaggio */
	n = size = 0;
	while(n < LENGTHMSG){
		if((size = read( sc, msg + n, (LENGTHMSG - n) * sizeof( char ) )) <= 0){
			if(size == 0)
				errno = ENOTCONN;

			return -1;
		}

		n = n + size;
	}
	msg[LENGTHMSG] = '\0';

	/* converte la stringa in intero */
	length = (int)strtod( msg, NULL );

	/* legge al piu' 'length' caratteri per ottenere il messaggio */
	n = size = 0;
	while(n < length){
		if((size = read( sc, msg + n, (length - n) * sizeof( char ) )) <= 0){
			if(size == 0)
				errno = ENOTCONN;

			return -1;
		}

		n = n + size;
	}
	msg[n] = '\0';

	return n;
}

int createServerChannel()
{
	int sock;
	struct sockaddr_in server;

	server.sin_family = AF_INET;
	server.sin_addr.s_addr = htonl( INADDR_ANY );
	server.sin_port = htons( PORT );

	/* crea la socket di rete */
	if((sock = socket( AF_INET, SOCK_STREAM, 0 )) == -1)
		return -1;

	if(bind( sock, (struct sockaddr *)&server, sizeof( server ) ) == -1){
		close( sock );
		return -1;
	}

	/* numero massimo di connessioni accettate contemporaneamente nella coda */
	if(listen( sock, SOMAXCONN ) == -1){
		close( sock );
		return -1;
	}

	return sock;
}

int acceptConnection( int s, char *ip, time_t date )
{
	char clientAddr[IPv6];
	int value;
	struct sockaddr_in client_addr;
	socklen_t client_len;

	if(s == -1){
		errno = EINVAL;
		return -1;
	}

	client_len = sizeof( client_addr );

	if((value = accept( s,(struct sockaddr*) &client_addr, &client_len )) == -1)
		return -1;

	date = time( NULL );
	strcpy( ip, inet_ntop( AF_INET, &client_addr.sin_addr, clientAddr, INET_ADDRSTRLEN ) );

	fprintf( stderr, "Client [ %s ] wants to connect on: %s", ip, asctime( localtime( &date ) ) );

	return value;
}

int closeSocket( int s )
{
	if(s == -1){
		errno = EINVAL;
		return -1;
	}

	if(close( s ) == -1)
		return -1;

	return 0;
}
