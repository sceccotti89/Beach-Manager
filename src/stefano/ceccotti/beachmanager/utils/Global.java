
package stefano.ceccotti.beachmanager.utils;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.engine.BeachManager;
import stefano.ceccotti.beachmanager.engine.NetworkManager;
import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * Contiene i dati globali di gioco
*/
public class Global
{
	/** gestore della socket */
	public static NetworkManager net;
	/** gestore della spiaggia */
	public static BeachManager bm;
	/** mantiene la lista delle modifiche della spiaggia */
	public static ArrayList<Log> logs;
	/** determina se l'applicazione e' in background */
	public static boolean isInBackground = false;
	/** determina se e' connesso al server */
	public static boolean connected = false;
	/** determina se la modalita' edit e' attiva */
	public static boolean edit_mode = false;
	/** numero totale di cabine */
	public static int cabins;
	/** numero totale di sdraie */
	public static int deckchairs;
	/** numero di cabine libere */
	public static int free_cabins;
	/** numero di sdraie libere */
	public static int free_deckchairs;
	/** prezzo di una cabina */
	public static float prezzo_cabina;
	/** prezzo di una sdraia */
	public static float prezzo_sdraia;

	/** numero massimo di colonne della spiaggia*/
	public static final int MAX_ROWS = 100;
	/** numero massimo di righe della spiaggia */
	public static final int MAX_COLUMNS = 100;

	/***************************************  GESTIONE DISPLAY  ************************************/

	/** lunghezza di una casella */
	public static float sizewBox;
	/** altezza di una casella */
	public static float sizehBox;
	/** lunghezza del display */
	public static float WIDTH;
	/** altezza del display */
	public static float HEIGHT;
	/** rapporto tra la lunghezza dello schermo attuale con quello standard */
	public static float ratioW;
	/** rapporto tra l'altezza dello schermo attuale con quello standard */
	public static float ratioH;

	/** numero di righe del display */
	private static final int rows = 18;
	/** numero di colonne del display */
	private static final int columns = 24;	
	/** lunghezza standard del display */
	private static final float baseWidth = 320;
	/** altezza standard del display */
	private static final float baseHeight = 480;

	/***********************************************************************************************/

	/** posizione X iniziale della griglia */
	public static float startX;
	/** posizione Y iniziale della griglia */
	public static float offsetY;

	/** confronta due date
	 * 
	 * @param first - prima data
	 * @param second - seconda data
	 * 
	 * @return -1 se first e' piu' grande di second, 0 se uguali, 1 se second e' maggiore di first
	*/
	public static int compare_dates( String first, String second )
	{
		first = first.substring( 3, 5 ) + first.substring( 0, 2 );
		second = second.substring( 3, 5 ) + second.substring( 0, 2 );

		char f, s;
		for(int i = 0; i < 4; i++){
			f = first.charAt( i );
			s = second.charAt( i );

			if(f > s)
				return -1;

			if(s > f)
				return 1;
		}

		return 0;
	}

	/** inserisce un nuovo log
	 * 
	 * @param log - il log da aggiungere
	*/
	public static synchronized void addLog( Log log )
	{
		// inserisce in testa
		logs.add( 0, log );
	}

	/** restituisce l'ID di un log
	 * 
	 * @param index - indice del log selezionato
	*/
	public static synchronized int getLogID( int index )
	{
		return logs.get( index ).getID();
	}

	/** rimuove un log
	 * 
	 * @param ID - ID del log da rimuovere
	*/
	public static synchronized void removeLog( int ID )
	{
		for(int i = logs.size() - 1; i >= 0; i--){
			if(logs.get( i ).getID() == ID){
				logs.remove( i );
				break;
			}
		}
	}

	/** imposta le dimensioni degli oggetti del display
	 * 
	 * @param main - l'activity principale
	 * @param orientation_changed - TRUE se e' cambiata orientazione, FALSE altrimenti
	*/
	public static void init( Activity main, boolean orientation_changed )
	{
		if(orientation_changed){
			float tmp = WIDTH;
			WIDTH = HEIGHT;
			HEIGHT = tmp;
		}
		else{
			DisplayMetrics displaymetrics = new DisplayMetrics();
			main.getWindowManager().getDefaultDisplay().getMetrics( displaymetrics );

			WIDTH = displaymetrics.widthPixels;
			HEIGHT = displaymetrics.heightPixels;
		}

		if(logs == null)
			logs = new ArrayList<Log>( 32 );

		// calcola le dimensioni di una casella
		sizewBox = WIDTH / columns;
		sizehBox = HEIGHT / rows;

		startX = Global.sizewBox * 2;
		offsetY = sizehBox;

		// calcola il rapporto tra le dimensioni standard e quelle nuove
		ratioW = (WIDTH / baseWidth);
		ratioH = (HEIGHT / baseHeight);
	}
}