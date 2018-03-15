
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <errno.h>
#include <time.h>

#include "dataThread.h"
#include "comsock.h"
#include "sqlQuery.h"
#include "mysql/mysql.h"
#include "errors.h"

/** dati per connettersi al database */
static const char *HOST = "localhost";
static const char *DB_USER = "c5game";
static const char *DB_PASS = "merendaio";
static const char *DATABASE = "beach";

/** numero di cabine e sdraie libere */
static unsigned int cabine_libere, sdraie_libere;

/** effettua una connessione al database
 *
 * @param mysql - puntatore all'oggetto di connessione al server
 *
 * @return il risultato della connessione se tutto e' andato bene, NULL altrimenti
*/
static MYSQL* SQL_connectDatabase( MYSQL *mysql )
{
	MYSQL *result;

	if(mysql == NULL){
		errno = EINVAL;
		return NULL;
	}

	/* inzializza l'oggetto per la connessione */
    ec_null_1( mysql_init( mysql ), "mysql_init", return NULL );

	/* si connette al database */
    ec_null_2( result = mysql_real_connect( mysql, HOST, DB_USER, DB_PASS, DATABASE, 0, NULL, 0 ), "mysql_real_connect", mysql_close( mysql ), return NULL );

	return result;
}

/** chiude la comunicazione al database
 *
 * @param mysql - puntatore alla struttura al database
 * @param res_con - puntatore alla connessione del database
*/
#define CLOSE_CONNECTION( mysql, res_con )	\
	mysql_close( (mysql) );					\
	mysql_close( (res_con) );

int SQL_getData( unsigned int *cabins, float *price_cabin, unsigned int *deckchairs, float *price_deckchair )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;
	MYSQL_ROW row;
	unsigned int i, n_rows;

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	ec_nonzero_2( mysql_query( &mysql, "SELECT `Cabine`, `Prezzo Cabina`, `Sdraie`, `Prezzo Sdraia` FROM StatusServer" ), "mysql_query1_getData",
																														  CLOSE_CONNECTION( &mysql, res_con ),
																														  return ERROR_QUERY );
	result = mysql_store_result( &mysql );

	row = mysql_fetch_row( result );
	cabine_libere = *cabins = strtol( row[0], NULL, 0 );
	*price_cabin = strtof( row[1], NULL );
	sdraie_libere = *deckchairs = strtol( row[2], NULL, 0 );
	*price_deckchair = strtof( row[3], NULL );

	mysql_free_result( result );

	/* controlla il numero di cabine e sdraie gia' prese e le aggiorna */
	ec_nonzero_2( mysql_query( &mysql, "SELECT Cabine, Sdraie FROM Prenotazioni WHERE Cabine > '0' OR Sdraie > '0'" ), "mysql_query2_getData", CLOSE_CONNECTION( &mysql, res_con ),
																																			   return ERROR_QUERY );

	result = mysql_store_result( &mysql );
	n_rows = mysql_num_rows( result );

	for(i = 0; i < n_rows; i++){
		row = mysql_fetch_row( result );

		cabine_libere = cabine_libere - strtol( row[0], NULL, 0 );
		sdraie_libere = sdraie_libere - strtol( row[1], NULL, 0 );

		mysql_field_seek( result, 0 );
	}

	mysql_free_result( result );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_isRegisted( const char *account, const char *password, char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;
	unsigned int found;

	if(account == NULL || password == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	/* crea ed esegue la query */
	snprintf( query, N, "SELECT IDLogin FROM Login WHERE Username = '%s' AND Password = '%s';", account, password );
	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query_isRegisted", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	result = mysql_store_result( &mysql );
	found = mysql_num_rows( result );

	/* se l'account e' corretto ottiene l'ID associato nel database */
	if(found > 0)
		found = strtol( (mysql_fetch_row( result ))[0], NULL, 0 );

	mysql_free_result( result );
	CLOSE_CONNECTION( &mysql, res_con );

	return found;
}

/** macro per il controllo di cancellazione delle date di prenotazione */
#define CHECK_REMOVE_DATE( c_day, c_month, c_year, l_day, l_month, l_year )				\
				((c_year) > (l_year)) ||												\
				((c_year) == (l_year) && (c_month) > (l_month)) ||						\
				((c_year) == (l_year) && (c_month) == (l_month) && (c_day) > (l_day))

int SQL_check_booking( char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;
	MYSQL_ROW row;
	int r_value = 0;
	unsigned int i, n_rows;
	time_t date;
	struct tm *currentTime;
	int c_day, c_month, c_year;
	int l_day, l_month, l_year;

	if(query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	date = time( NULL );
	currentTime = localtime( &date );

	c_day = currentTime->tm_mday;
	c_month = currentTime->tm_mon + 1;
	c_year = 1900 + currentTime->tm_year;

	ec_nonzero_2( mysql_query( &mysql, "SELECT `IDPrenotazione`, `IDPlace`, `DataFine`, `Cabine`, `Sdraie` FROM Prenotazioni;" ),
				  "mysql_query1_check_booking",
				  CLOSE_CONNECTION( &mysql, res_con ),
				  return ERROR_QUERY );

	result = mysql_store_result( &mysql );
	n_rows = mysql_num_rows( result );

	for(i = 0; i < n_rows; i++){
		row = mysql_fetch_row( result );

		l_day = strtol( row[2], NULL, 10 );
		l_month = strtol( row[2] + 3, NULL, 10 );
		l_year = strtol( row[2] + 6, NULL, 10 );

		/* controlla se rimuovere la prenotazione */
		if(CHECK_REMOVE_DATE( c_day, c_month, c_year, l_day, l_month, l_year )){
			snprintf( query, N, "DELETE FROM Prenotazioni WHERE IDPrenotazione = '%s';", row[0] );
			ec_nonzero_1( mysql_query( &mysql, query ), "mysql_query2_check_booking", break );

			/* invia l'aggiornamento ai client */
			snprintf( query, N, "%c\n%s\n%s\n", MSG_DELETE_BOOKING, row[1], row[0] );
			if((r_value = multicast( -1, query )) < 0)
				break;

			/* aggiorna il numero di cabine e sdraie libere */
			cabine_libere = cabine_libere + strtol( row[3], NULL, 0 );
			sdraie_libere = sdraie_libere + strtol( row[4], NULL, 0 );

			snprintf( query, N, "UPDATE Cabine SET `IDPrenotazione` = '0' WHERE IDPrenotazione = '%s';", row[0] );
			ec_nonzero_1( mysql_query( &mysql, query ), "mysql_query3_check_booking", break );
		}

		mysql_field_seek( result, 0 );
	}

	mysql_free_result( result );
	CLOSE_CONNECTION( &mysql, res_con );

	return r_value;
}

int SQL_sendDataToUser( unsigned int index, unsigned int fd, char *buffer, char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result1, *result2, *result3;
	MYSQL_ROW row1, row2, row3;
	unsigned int IDPlace, IDPrenotazione, cabine;
	unsigned int i, j, k, n, n_rows1, n_rows2;

	if(buffer == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	ec_nonzero_2( mysql_query( &mysql, "SELECT * FROM Beach ORDER BY Y, X;" ), "mysql_query1_sendData", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	/* ottiene il numero di ombrelloni */
	result1 = mysql_store_result( &mysql );
	n_rows1 = mysql_num_rows( result1 );

	/* invia al client quanti dati dovrà ricevere */
	snprintf( buffer, N, "%d\n", n_rows1 );
	sendMessage( fd, buffer );

	for(i = 0; i < n_rows1; i++){
		row1 = mysql_fetch_row( result1 );

		IDPlace = strtol( row1[0], NULL, 0 );

		snprintf( buffer, N, "%c\n%d\n%s\n%s\n%s\n%s\n", MSG_ADD_PLACE, IDPlace, row1[1], row1[2], row1[3], row1[4] );
		sendMessage( fd, buffer );

		snprintf( query, N, "SELECT `IDPrenotazione`, `DataInizio`, `DataFine`, `Nome`, `Cognome`, `Telefono`, `Cabine`, `Sdraie` FROM Prenotazioni WHERE IDPlace = '%u';", IDPlace );
		ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query2_sendData", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

		result2 = mysql_store_result( &mysql );
		n_rows2 = mysql_num_rows( result2 );

		for(j = 0; j < n_rows2; j++){
			row2 = mysql_fetch_row( result2 );

			IDPrenotazione = strtol( row2[0], NULL, 0 );
			cabine = strtol( row2[6], NULL, 0 );
			snprintf( buffer, N, "%c\n%u\n%s\n%s\n%s\n%s\n%s\n%u\n%s\n", MSG_ADD_BOOKING, IDPrenotazione, row2[1], row2[2], row2[3], row2[4], row2[5], cabine, row2[7] );

			/* ottiene gli ID delle cabine prenotate */
			if(cabine > 0){
				snprintf( query, N, "SELECT `IDCabina` FROM Cabine WHERE IDPrenotazione = '%u';", IDPrenotazione );
				ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query3_sendData", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

				result3 = mysql_store_result( &mysql );

				for(k = 0; k < cabine; k++){
					row3 = mysql_fetch_row( result3 );
					n = strlen( buffer );
					snprintf( buffer + n, N - n - 1, "%s\n", row3[0] );
					mysql_field_seek( result3, 0 );
				}
			}

			sendMessage( fd, buffer );

			mysql_field_seek( result2, 0 );
		}

		mysql_free_result( result2 );

		mysql_field_seek( result1, 0 );
	}

	mysql_free_result( result1 );

	/* invia i dati relativi alle tariffe */
	snprintf( query, N, "SELECT IDPacchetto, DataInizio, DataFine, DailyPrice, WeeklyPrice FROM Pacchetti;" );
	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query4_sendData", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	/* ottiene il numero di pacchetti */
	result1 = mysql_store_result( &mysql );
	n_rows1 = mysql_num_rows( result1 );

	for(i = 0; i < n_rows1; i++){
		row1 = mysql_fetch_row( result1 );

		snprintf( buffer, N, "%c\n%s\n%s\n%s\n%s\n%s\n", MSG_ADD_TARIFF, row1[0], row1[1], row1[2], row1[3], row1[4] );
		sendMessage( fd, buffer );

		mysql_field_seek( result1, 0 );
	}

	mysql_free_result( result1 );

	data[index].ready = 1;

	snprintf( buffer, N, "%c\n", MSG_FINISH );
	sendMessage( fd, buffer );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_delete_all()
{
	MYSQL mysql, *res_con;

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	ec_nonzero_2( mysql_query( &mysql, "TRUNCATE Beach;" ), "mysql_query1_delete_all", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );
	ec_nonzero_2( mysql_query( &mysql, "UPDATE Cabine SET `IDPrenotazione` = '0';" ), "mysql_query2_delete_all", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );
	ec_nonzero_2( mysql_query( &mysql, "TRUNCATE Prenotazioni;" ), "mysql_query3_delete_all", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

/** aggiorna lo stato delle cabine
 *
 * @param cabins - numero di cabine prenotate
 * @param old_cabins - numero delle vecchie cabine prenotate
 * @param IDPrenotazione - ID della prenotazione
 * @param mysql - oggetto per la connessione al database
 * @param buffer - area per inserire gli ID delle cabine prenotate
 * @param query - buffer per l'esecuzione della query
 *
 * @return 1 se tutto e' andato bene, < 0 altrimenti
*/
static int update_cabins( unsigned int cabins, unsigned int old_cabins, unsigned int IDPrenotazione, MYSQL mysql, char *buffer, char *query )
{
	MYSQL_RES *result;
	MYSQL_ROW row;
	unsigned int i, n;

	if(buffer == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if(cabins > old_cabins){
		snprintf( query, N, "SELECT IDCabina FROM Cabine WHERE IDPrenotazione = '0' LIMIT %u;", cabins - old_cabins );
		ec_nonzero_1( mysql_query( &mysql, query ), "mysql_query1_update_cabins", return ERROR_QUERY );

		result = mysql_store_result( &mysql );

		n = 0;
		for(i = 0; i < cabins - old_cabins; i++){
			row = mysql_fetch_row( result );

			snprintf( query, N, "UPDATE Cabine SET `IDPrenotazione` = '%u' WHERE `IDCabina` = '%s';", IDPrenotazione, row[0] );
			ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query2_update_cabins", mysql_free_result( result ), return ERROR_QUERY );

			/* salva l'ID della cabina libera */
			snprintf( buffer + n, N - n - 1, "%s\n", row[0] );
			n = strlen( buffer );

			mysql_field_seek( result, 0 );
		}

		mysql_free_result( result );
	}
	else{
		if(cabins < old_cabins){
			snprintf( query, N, "UPDATE Cabine SET `IDPrenotazione` = '0' WHERE `IDPrenotazione` = '%u' LIMIT %u", IDPrenotazione, old_cabins - cabins );
			ec_nonzero_1( mysql_query( &mysql, query ), "mysql_query3_update_cabins", return ERROR_QUERY );
		}
	}

	return 1;
}

/** macro per il controllo delle date di prenotazione */
#define CHECK_DATE( c_f_day, c_f_month, c_f_year, c_t_day, c_t_month, c_t_year, l_f_day, l_f_month, l_f_year, l_t_day, l_t_month, l_t_year ) \
		((((l_f_year) > (c_f_year)) ||																		\
		 ((l_f_year) == (c_f_year) && (l_f_month) > (c_f_month)) ||											\
		 ((l_f_year) == (c_f_year) && (l_f_month) == (c_f_month) && (l_f_day) >= (c_f_day))) &&				\
		(((l_f_year) < (c_t_year)) ||																		\
		 ((l_f_year) == (c_t_year) && (l_f_month) < (c_t_month)) ||											\
		 ((l_f_year) == (c_t_year) && (l_f_month) == (c_t_month) && (l_f_day) <= (c_t_day)))) ||			\
		((((l_t_year) > (c_f_year)) ||																		\
		 ((l_t_year) == (c_f_year) && (l_t_month) > (c_f_month)) ||											\
		 ((l_t_year) == (c_f_year) && (l_t_month) == (c_f_month) && (l_t_day) >= (c_f_day))) &&				\
		(((l_t_year) < (c_t_year)) ||																		\
		 ((l_t_year) == (c_t_year) && (l_t_month) < (c_t_month)) ||											\
		 ((l_t_year) == (c_t_year) && (l_t_month) == (c_t_month) && (l_t_day) <= (c_t_day)))) ||			\
		((((l_f_year) < (c_f_year)) ||																		\
		 ((l_f_year) == (c_f_year) && (l_f_month) < (c_f_month)) ||											\
		 ((l_f_year) == (c_f_year) && (l_f_month) == (c_f_month) && (l_f_day) <= (c_f_day))) &&				\
		(((l_t_year) > (c_t_year)) ||																		\
		 ((l_t_year) == (c_t_year) && (l_t_month) > (c_t_month)) ||											\
		 ((l_t_year) == (c_t_year) && (l_t_month) == (c_t_month) && (l_t_day) >= (c_t_day))))

int SQL_update_booking( unsigned int type, unsigned int *IDPrenotazione, unsigned int IDPlace, char *date_from, char *date_to,
						char *name, char *surname, char *phone, unsigned int old_cabins, unsigned int cabins, unsigned int old_deckchairs, unsigned int deckchairs,
						char *error, char *buffer, char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;
	MYSQL_ROW row;
	unsigned int i, n_rows;
	int c_f_day, c_f_month, c_f_year;
	int c_t_day, c_t_month, c_t_year;
	int l_f_day, l_f_month, l_f_year;
	int l_t_day, l_t_month, l_t_year;
	int value = 0;

	if((type != SQL_DELETE && date_from == NULL) || (type != SQL_DELETE && date_to == NULL) || (type != SQL_DELETE && name == NULL) ||
	   (type != SQL_DELETE && surname == NULL) || (type != SQL_DELETE && phone == NULL) || (type != SQL_DELETE && error == NULL) || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	/* in caso di cancellazione viene eseguita subito */
	if(type == SQL_DELETE){
		/* toglie la prenotazione alle cabine */
		snprintf( query, N, "UPDATE Cabine SET `IDPrenotazione` = '0' WHERE IDPrenotazione = '%u';", *IDPrenotazione );

		ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query1_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

		/* aggiorna le cabine e le sdraie libere */
		cabine_libere = cabine_libere + cabins;
		sdraie_libere = sdraie_libere + deckchairs;

		snprintf( query, N, "DELETE FROM Prenotazioni WHERE IDPrenotazione = '%u';", *IDPrenotazione );
		ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query2_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

		CLOSE_CONNECTION( &mysql, res_con );

		return 1;
	}

	/* controlla se il posto e' libero */
	snprintf( query, N, "SELECT `DataInizio`, `DataFine` FROM Prenotazioni WHERE IDPlace = '%u';", IDPlace );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query3_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );
	result = mysql_store_result( &mysql );

	/* per controllare se il posto e' ancora libero esegue dei controlli sulla data di inizio e fine di ciascuna occorrenza */
	n_rows = mysql_num_rows( result );

	c_f_day = strtol( date_from, NULL, 10 );
	c_f_month = strtol( date_from + 3, NULL, 10 );
	c_f_year = strtol( date_from + 6, NULL, 10 );
	c_t_day = strtol( date_to, NULL, 10 );
	c_t_month = strtol( date_to + 3, NULL, 10 );
	c_t_year = strtol( date_to + 6, NULL, 10 );

	for(i = 0; i < n_rows; i++){
		row = mysql_fetch_row( result );

		l_f_day = strtol( row[0], NULL, 10 );
		l_f_month = strtol( row[0] + 3, NULL, 10 );
		l_f_year = strtol( row[0] + 6, NULL, 10 );
		l_t_day = strtol( row[1], NULL, 10 );
		l_t_month = strtol( row[1] + 3, NULL, 10 );
		l_t_year = strtol( row[1] + 6, NULL, 10 );

		/* se una data e' nel periodo di prenotazione, non aggiunge niente */
		if(CHECK_DATE( c_f_day, c_f_month, c_f_year, c_t_day, c_t_month, c_t_year, l_f_day, l_f_month, l_f_year, l_t_day, l_t_month, l_t_year ))
			break;

		mysql_field_seek( result, 0 );
	}

	mysql_free_result( result );

	if(i == n_rows){
		/* controlla se il numero di cabine e sdraie sono sufficienti */
		if(cabins > old_cabins && cabine_libere < cabins - old_cabins)
			*error = NO_FREE_CABINS;
		else{
			if(deckchairs > old_deckchairs && sdraie_libere < deckchairs - old_deckchairs)
				*error = NO_FREE_DECKCHAIRS;
			else{
				/* essendo ancora libero inserisce o modifica una prenotazione */
				if(type == SQL_NEW)
					snprintf( query, N, "INSERT INTO Prenotazioni ( `IDPlace`, `DataInizio`, `DataFine`, `Nome`, `Cognome`, `Telefono`, `Cabine`, `Sdraie` ) VALUES ( '%d', '%s', '%s', '%s', '%s', '%s', '%d', '%d' );", IDPlace, date_from, date_to, name, surname, phone, cabins, deckchairs );
				else
					snprintf( query, N, "UPDATE Prenotazioni SET `DataInizio` = '%s', `DataFine` = '%s', `Nome` = '%s', `Cognome` = '%s', `Telefono` = '%s', `Cabine` = '%d', `Sdraie` = '%d' WHERE IDPrenotazione = '%u';", date_from, date_to, name, surname, phone, cabins, deckchairs, *IDPrenotazione );

				ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query4_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

				cabine_libere = cabine_libere - (cabins - old_cabins);
				sdraie_libere = sdraie_libere - (deckchairs - old_deckchairs);

				value = 1;
			}
		}
	}
	else
		*error = PLACE_ALREADY_BOOKED;

	if(value == 1){
		if(type == SQL_NEW){
			/* ottiene l'ID associato alla prenotazione */
			snprintf( query, N, "SELECT MAX( IDPrenotazione ) FROM Prenotazioni;" );
			ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query5_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

			result = mysql_store_result( &mysql );
			*IDPrenotazione = strtol( (mysql_fetch_row( result ))[0], NULL, 0 );
			mysql_free_result( result );

			if((value = update_cabins( cabins, old_cabins, *IDPrenotazione, mysql, buffer, query )) < 0){
				/* rimuove la prenotazione appena effettuata */
				SQL_update_booking( SQL_DELETE, IDPrenotazione, 0, NULL, NULL, NULL, NULL, NULL, 0, cabins, 0, deckchairs, error, buffer, query );

				CLOSE_CONNECTION( &mysql, res_con );

				return value;
			}
		}
		else{
			if((value = update_cabins( cabins, old_cabins, *IDPrenotazione, mysql, buffer, query )) < 0){
				/* rimuove la prenotazione appena effettuata */
				SQL_update_booking( SQL_DELETE, IDPrenotazione, 0, NULL, NULL, NULL, NULL, NULL, 0, cabins, 0, deckchairs, error, buffer, query );

				CLOSE_CONNECTION( &mysql, res_con );

				return value;
			}
		}
	}

	CLOSE_CONNECTION( &mysql, res_con );

	return value;
}

int SQL_add_place( unsigned int *IDPlace, unsigned int X, unsigned int Y, char *name, float price, char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;

	if(name == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	snprintf( query, N, "INSERT INTO `Beach` ( Y, X, Name, Value ) VALUES ( '%u', '%u', '%s', '%f' );", Y, X, name, price );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query1_add_place", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	/* ottiene l'ID associato al posto */
	snprintf( query, N, "SELECT MAX( IDPlace ) FROM Beach;" );
	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query2_add_place", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	result = mysql_store_result( &mysql );

	*IDPlace = strtol( (mysql_fetch_row( result ))[0], NULL, 0 );

	mysql_free_result( result );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_modify_place( unsigned int IDPlace, char *name, float price, char *query )
{
	MYSQL mysql, *res_con;

	if(name == NULL || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	snprintf( query, N, "UPDATE Beach SET `Name` = '%s', `Value` = '%f' WHERE IDPlace = '%u';", name, price, IDPlace );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query_modify_place", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_delete_place( unsigned int IDPlace, char *query )
{
	MYSQL mysql, *res_con;

	if(query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	snprintf( query, N, "DELETE FROM Beach WHERE IDPlace = '%u';", IDPlace );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query1_delete_place", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	snprintf( query, N, "DELETE FROM Prenotazioni WHERE IDPlace = '%u';", IDPlace );
	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query2_delete_place", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_update_tariff( unsigned int type, unsigned int *IDPacchetto, char *date_from, char *date_to, float daily_price, float weekly_price, char *query )
{
	MYSQL mysql, *res_con;
	MYSQL_RES *result;
	MYSQL_ROW row;
	unsigned int i, n_rows;
	int c_f_day, c_f_month;
	int c_t_day, c_t_month;
	int l_f_day, l_f_month;
	int l_t_day, l_t_month;
	int value = 0;

	if((type != SQL_DELETE && date_from == NULL) || (type != SQL_DELETE && date_to == NULL) || daily_price < 0 || weekly_price < 0 || query == NULL){
		errno = EINVAL;
		return ERROR_EINVAL;
	}

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	/* in caso di cancellazione viene eseguita subito */
	if(type == SQL_DELETE){
		snprintf( query, N, "DELETE FROM Pacchetti WHERE IDPacchetto = '%u';", *IDPacchetto );
		ec_nonzero_3( mysql_query( &mysql, query ), "mysql_query1_update_tariff", mysql_close( &mysql ), mysql_close( res_con ), return ERROR_QUERY );

		mysql_close( &mysql );
		mysql_close( res_con );

		return 1;
	}

	ec_nonzero_2( mysql_query( &mysql, "SELECT `DataInizio`, `DataFine` FROM Pacchetti;" ), "mysql_query2_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	result = mysql_store_result( &mysql );
	n_rows = mysql_num_rows( result );

	c_f_day = strtol( date_from, NULL, 10 );
	c_f_month = strtol( date_from + 3, NULL, 10 );
	c_t_day = strtol( date_to, NULL, 10 );
	c_t_month = strtol( date_to + 3, NULL, 10 );

	/* controlla se puo' inserire la tariffa */
	for(i = 0; i < n_rows; i++){
		row = mysql_fetch_row( result );

		l_f_day = strtol( row[0], NULL, 10 );
		l_f_month = strtol( row[0] + 3, NULL, 10 );
		l_t_day = strtol( row[1], NULL, 10 );
		l_t_month = strtol( row[1] + 3, NULL, 10 );

		/* se una data e' nel periodo della tariffa, non aggiunge niente */
		if(CHECK_DATE( c_f_day, c_f_month, 0, c_t_day, c_t_month, 0, l_f_day, l_f_month, 0, l_t_day, l_t_month, 0 ))
			break;

		mysql_field_seek( result, 0 );
	}

	if(i == n_rows){
		/* essendo ancora libero inserisce o modifica una tariffa */
		if(type == SQL_NEW)
			snprintf( query, N, "INSERT INTO Pacchetti ( `DataInizio`, `DataFine`, `DailyPrice`, `WeeklyPrice` ) VALUES ( '%s', '%s', '%f', '%f' );", date_from, date_to, daily_price, weekly_price );
		else
			snprintf( query, N, "UPDATE Pacchetti SET `DataInizio` = '%s', `DataFine` = '%s', `DailyPrice` = '%f', `WeeklyPrice` = '%f' WHERE IDPacchetto = '%u';",
																														date_from, date_to, daily_price, weekly_price, *IDPacchetto );

		ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query3_update_booking", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

		value = 1;
	}

	if(type == SQL_NEW){
		/* ottiene l'ID associato alla tariffa */
		ec_nonzero_2( mysql_query( &mysql, "SELECT MAX( IDPacchetto ) FROM Pacchetti;" ), "mysql_query4_update_tariff", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

		result = mysql_store_result( &mysql );
		*IDPacchetto = strtol( (mysql_fetch_row( result ))[0], NULL, 0 );

		mysql_free_result( result );
	}

	mysql_close( &mysql );
	mysql_close( res_con );

	return value;
}

int SQL_modify_data( unsigned int cabins, unsigned int *old_cabins, float price_cabin, float *old_price_cabin,
					 unsigned int deckchairs, unsigned int *old_deckchairs, float price_deckchair, float *old_price_deckchair, char *query )
{
	MYSQL mysql, *res_con;

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	snprintf( query, N, "UPDATE StatusServer SET `Cabine` = '%d', `Prezzo Cabina` = '%f', `Sdraie` = '%d', `Prezzo Sdraia` = '%f' WHERE IDServer = '1';", cabins, price_cabin,
																																						  deckchairs, price_deckchair );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query_modify_data", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	/* aggiorna il numero di sdraie e cabine libere */
	cabine_libere = cabine_libere - (cabins - *old_cabins);
	sdraie_libere = sdraie_libere - (deckchairs - *old_deckchairs);

	*old_cabins = cabins;
	*old_deckchairs = deckchairs;

	*old_price_cabin = price_cabin;
	*old_price_deckchair = price_deckchair;

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}

int SQL_updateServerStatus( int online )
{
	time_t date;
	MYSQL mysql, *res_con;
	char query[N];

	if((res_con = SQL_connectDatabase( &mysql )) == NULL){
		fprintf( stderr, "Database not found\n" );
		return ERROR_CONNECTION_DB;
	}

	if(online){
		/* aggiorna la data di attivazione del server */
		date = time( NULL );
		snprintf( query, N, "UPDATE StatusServer SET `DataOnline` = '%s' WHERE IDServer = '1';", asctime( localtime( &date ) ) );
	}
	else	/* se il server e' offline viene aggiornata solo la data */
		snprintf( query, N, "UPDATE StatusServer SET `DataOnline` = '-' WHERE IDServer = '1';" );

	ec_nonzero_2( mysql_query( &mysql, query ), "mysql_query_uploadServerStatus", CLOSE_CONNECTION( &mysql, res_con ), return ERROR_QUERY );

	CLOSE_CONNECTION( &mysql, res_con );

	return 0;
}
