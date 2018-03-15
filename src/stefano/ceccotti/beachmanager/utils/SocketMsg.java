
package stefano.ceccotti.beachmanager.utils;

/**
 * tipi di messaggio inviati tra client e server
*/
public class SocketMsg
{
	/** account, o prenotazione, inserito correttamente */
	public static final char OK	=					'A';
	/** account sbagliato o prenotazione rifiutata */
	public static final char NO	=					'B';
	/** invia i dati di un oggetto */
	public static final char DATA =					'C';
	/** nessuna prenotazione */
	public static final char NO_BOOKING =			'D';
	/** fine ricezione dati degli ombrelloni */
	public static final char FINISH =				'E';
	/** cancella tutti gli ombrelloni */
	public static final char DELETE_ALL_PLACES =	'F';
	/** aggiunge una prenotazione a un ombrellone */
	public static final char ADD_BOOKING =			'G';
	/** modifica una prenotazione */
	public static final char MODIFY_BOOKING =		'H';
	/** elimina una prenotazione */
	public static final char DELETE_BOOKING =		'I';
	/** aggiunge un ombrellone */
	public static final char ADD_PLACE =			'J';
	/** modifica un ombrellone */
	public static final char MODIFY_PLACE =			'K';
	/** elimina un ombrellone */
	public static final char DELETE_PLACE =			'L';
	/** aggiunge una nuova tariffa */
	public static final char ADD_TARIFF =			'M';
	/** modifica una tariffa */
	public static final char MODIFY_TARIFF =		'N';
	/** elimina una tariffa */
	public static final char DELETE_TARIFF =		'O';
	/** modifica il numero di cabine e sdraie */
	public static final char MODIFY_DATA =			'P';
	/** si e' verificato un problema nel server */
	public static final char ERROR_SERVER =			'Q';
	/** il server e' down */
	public static final char SERVER_DOWN =			'R';
}