
package stefano.ceccotti.beachmanager.engine;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

/**
 * Gestisce i messaggi in entrata e in uscita dalla socket
*/
public class NetworkManager extends BroadcastReceiver
{
	/* socket usata per connettersi col server */
	private Socket socket = null;
	/* buffer di lettura dalla socket */
	private BufferedReader in = null;
	/* stream di scrittura della socket */
	private DataOutputStream out = null;
	/* tentativi di connessione col server */
	private int trial;
	/* determina se eseguire la connessione */
	private boolean connecting = false;
	/* account e password inseriti dall'utente */
	private String account, password;
	/* thread dedicato alla ricezione dei dati dal server */
	private DataReceiver data;
	/* indirizzo di rete del server */
	private String network_address;

	/* numero di tentativi di connessione col server */
	private static final int NTRIALCONN = 5;
	/* versione attuale del client */
	private static final double VERSION = 1.0;
	/* porta di comunicazione col server */
	private static final int PORT = 9000;
	/* stati della rete */
	public static final int NETWORK_DEVICES_OFF = 0, NETWORK_CONNECTED = 1, NETWORK_DISCONNECTED = 2;

	public NetworkManager( Context context, Handler handler, DataReceiver data )
	{
		trial = NTRIALCONN;

		this.data = data;

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction( ConnectivityManager.CONNECTIVITY_ACTION );
		context.registerReceiver( this, intentFilter );
	}

	/** chiude la connessione in corso */
	public void closeConnection()
	{
		try{ socket.close(); }
		catch( Exception e ){}
		try{ in.close(); }
		catch( Exception e ){}
		try{ out.close(); }
		catch( Exception e ){}

		in = null;
		out = null;
		socket = null;

		resetStats();
	}

	/** resetta i parametri ma non la connessione (se attiva) */
	public void resetStats()
	{
		connecting = false;
		trial = NTRIALCONN;
	}

	@Override
	public void onReceive( Context context, Intent intent )
	{
		int state = this.getNetworkStatus( context );
		// TODO gestire meglio i cambi di stato della rete
		if(isConnected() && state != NETWORK_CONNECTED)
			System.out.println( "SONO QUI" ); // TODO notifica che la connessione e' stata persa
	}

	/** restituisce lo stream di scrittura */
	public DataOutputStream getOut()
	{
		return out;
	}

	/** attende di essere risvegliato appena effettuata la connessione */
	public synchronized void waitConnection()
	{
		while(socket == null){
			try{ wait(); }
			catch( InterruptedException e ){ e.printStackTrace(); }
		}
	}

	/** assegna l'indirizzo di rete del server
	 * 
	 * @param address - indirizzo di rete del server
	*/
	public void setNetworkAddress( String address )
	{
		network_address = address;
	}

	/** ottiene lo stato della rete mobile (3G/4G) e wifi
	 * 
	 * @param context - il contesto da cui caricare i dati
	 * 
	 * @return uno tra: NETWORK_DEVICES_OFF, NEWTORK_CONNECTED, NETWORK_DISCONNECTED
	*/
	public int getNetworkStatus( Context context )
	{
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );

		NetworkInfo wifi = manager.getNetworkInfo( ConnectivityManager.TYPE_WIFI );
		NetworkInfo mobile = manager.getNetworkInfo( ConnectivityManager.TYPE_MOBILE );

		// controlla se il wifi o la rete mobile (3G/4G) e' attiva
		if(!wifi.isAvailable() && !mobile.isAvailable())
			return NETWORK_DEVICES_OFF;
		else{
			// controlla se ha stabilito una connessione
			if(wifi.isConnected() || mobile.isConnected())
				return NETWORK_CONNECTED;
		    else // nessuna rete
		    	return NETWORK_DISCONNECTED;
		}
	}

	/** avvia la connessione al server
	 * 
	 * @param account - l'account
	 * @param password - la password
	*/
	public void startConnection( String account, String password )
	{
		this.account = DataCripted.encrypt( account );
		this.password = DataCripted.encrypt( password );

		connecting = true;

		data.activate_thread();
	}

	/** ottiene il prossimo messaggio nella socket */
	public String getMessage() throws IOException
	{
		return in.readLine();
	}

	/** ottiene il prossimo carattere nella socket */
	public char getChar() throws IOException
	{
		return in.readLine().charAt( 0 );
	}

	/** ottiene il prossimo intero nella socket */
	public int getInt() throws Exception
	{
		return Integer.parseInt( in.readLine() );
	}

	/** ottiene il prossimo numero decimale nella socket */
	public float getFloat() throws Exception
	{
		return Float.parseFloat( in.readLine() );
	}

	/** invia un messaggio (o piu' messaggi) tramite la socket
	 * 
	 * @param messages - lista di messaggi da spedire
	*/
	public void sendMessage( String... messages )
	{
		// il messaggio da inviare
		String message = "";

		int length = messages.length;
		for(int i = 0; i < length; i++){
			if(messages[i].length() < 10)
				message = message + "00";
			else
				if(messages[i].length() < 100)
					message = message + "0";
			message = message + messages[i].length() + messages[i];
		}

		try{
			out.writeBytes( message );
			out.flush();
		}catch( IOException e ){ e.printStackTrace(); }
	}

	/** determina se si sta connettendo
	 * 
	 * @return TRUE se si sta connettendo, FALSE altrimenti
	*/
	public boolean isConnecting()
	{
		return connecting;
	}

	/** determina se si e' connesso al server
	 * 
	 * @return TRUE se si e' connesso, FALSE altrimenti
	*/
	public boolean isConnected()
	{
		return socket != null;
	}

	/** tenta di stabilire una connessione col server */
	public synchronized void connection()
	{
		// controlla se il collegamento e' gia' stato fatto
		if(socket != null){
			sendMessage( account, password );
			resetStats();

			return;
		}

		try{ socket = new Socket( network_address, PORT ); }
		catch( Exception e ){}

		if(socket == null){
			// tentativo fallito
			if(--trial == 0)
				resetStats();
		}
		else{
			// connessione riuscita
			try{
				in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
				out = new DataOutputStream( socket.getOutputStream() );
			}
			catch( IOException e ){ e.printStackTrace(); }

			sendMessage( VERSION + "", account, password );
			resetStats();

			notifyAll();
		}
	}
}