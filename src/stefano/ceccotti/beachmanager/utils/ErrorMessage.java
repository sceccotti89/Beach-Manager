
package stefano.ceccotti.beachmanager.utils;

import stefano.ceccotti.beachmanager.R;
import android.app.Activity;

public class ErrorMessage
{
	/** account non esistente */
	private final static char ACCOUNT_NOT_EXIST = 'A';
	/** account gia' in uso */
	private final static char ACCOUNT_ALREADY_USED = 'B';
	/** il server e' pieno */
	private final static char SERVER_FULL = 'C';
	/** versione vecchia dell'applicazione */
	private final static char OLD_VERSION = 'D';
	/** non ci sono piu' cabine libere */
	private final static char NO_FREE_CABINS = 'E';
	/** non ci sono piu' sdraie libere */
	private final static char NO_FREE_DECKCHAIRS = 'F';
	/** il posto selezionato e' gia' prenotato */
	private final static char PLACE_ALREADY_BOOKED = 'G';
	/** impossibile connettersi al server */
	public final static char UNABLE_TO_CONNECT = 'H';

	/** restituisce il testo dell'errore associato
	 * 
	 * @param type - il tipo di errore
	 * @param activity - l'activity per recuperare la stringa
	 * 
	 * @return la stringa associata all'errore
	*/
	public static String getError( char type, Activity activity )
	{
		switch( type ){
			case( ACCOUNT_NOT_EXIST ):
				return activity.getString( R.string.account_dont_exist );
			case( ACCOUNT_ALREADY_USED ):
				return activity.getString( R.string.account_already_used );
			case( SERVER_FULL ):
				return activity.getString( R.string.server_full );
			case( OLD_VERSION ):
				return activity.getString( R.string.old_version );
			case( NO_FREE_CABINS ):
				return activity.getString( R.string.no_free_cabins );
			case( NO_FREE_DECKCHAIRS ):
				return activity.getString( R.string.no_free_deckchairs );
			case( PLACE_ALREADY_BOOKED ):
				return activity.getString( R.string.place_already_booked );
			case( UNABLE_TO_CONNECT ):
				return activity.getString( R.string.unable_to_connect );
		}

		return null;
	}
}