
package stefano.ceccotti.beachmanager.engine;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.BookingActivity;
import stefano.ceccotti.beachmanager.RateActivity;
import stefano.ceccotti.beachmanager.utils.ErrorMessage;
import stefano.ceccotti.beachmanager.utils.Global;
import stefano.ceccotti.beachmanager.utils.SocketMsg;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Thread dedicato alla ricezione dei dati dal server
*/
public class DataReceiver implements Runnable
{
	/* determina se avviare la connessione */
	private boolean try_connection = false;
	/* handler per lo scambio di messaggi */
	private static Handler handler;
	/* gestore della rete */
	private NetworkManager net;
	/* determina se e' stato chiuso dall'utente */
	public static boolean closeFromUser = false;

	public DataReceiver( Context context, Handler hand )
	{
		handler = hand;
		Global.net = net = new NetworkManager( context, handler, this );
	}

	/** assegna un nuovo handler
	 * 
	 * @param hand - il nuovo handler
	*/
	public synchronized static void setHandler( Handler hand )
	{
		handler = hand;
	}

	/** avvia il thread per la connessione */
	public synchronized void activate_thread()
	{
		try_connection = true;

		notifyAll();
	}

	/** attende di essere risvegliato */
	private synchronized void wait_for_start() throws InterruptedException
	{
		while(!try_connection)
			wait();
	}

	/** avvia la connessione al server */
	private void start_connection()
	{
		while(true){
			net.connection();

			if(!net.isConnecting())
				break;
			else{
				try{ Thread.sleep( 1000 ); }
				catch( Exception e ){ e.printStackTrace(); }
			}
		}

		try_connection = false;

		if(closeFromUser)
			closeFromUser = false;
		else{
			if(!net.isConnected())
				notifyMessage( SocketMsg.NO, ErrorMessage.UNABLE_TO_CONNECT + "" );
		}
	}

	@Override
	public void run()
	{
		char type;
		boolean close;
		int cabine, old_cabine;
		String IDPlace = null;
		final String contents[] = new String[0];
		ArrayList<String> message = new ArrayList<String>();

		while(true){
			try{
				// ================ TODO TEST =============== //
				/*Thread.sleep( 4000 );
				notifyMessage( SocketMsg.OK, "72" );

				int sleep = 150;

				int ID = 1;
				for(int i = 1; i <= 12; i++){
					for(int j = 1; j <= 6; j++){
						notifyMessage( SocketMsg.ADD_PLACE, ID + "", i + "", j + "", ((i + 1) * (j + 1)) + "", "1" );
						Thread.sleep( sleep );

						ID++;
					}
				}

				/*notifyMessage( SocketMsg.ADD_PLACE, "1", "1", "1" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "2", "1", "2" );
				notifyMessage( SocketMsg.ADD_BOOKING, "2", "1", "13-10-2015", "13-10-2015", "Tommaso", "Catuogno", "0571/377750", "10" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "3", "1", "3" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "4", "2", "1" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "5", "3", "2" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "6", "4", "1" );
				notifyMessage( SocketMsg.ADD_BOOKING, "6", "2", "22-11-2014", "25-11-2014", "Tommaso", "Catuogno", "0571/377750", "10" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "7", "7", "5" );
				Thread.sleep( sleep );
				notifyMessage( SocketMsg.ADD_PLACE, "8", "9", "6" );

				notifyMessage( SocketMsg.FINISH );

				Thread.sleep( 4000 );
				notifyMessage( SocketMsg.DELETE_PLACE, "8" );
				Thread.sleep( 2000 );
				notifyMessage( SocketMsg.DELETE_PLACE, "7" );
				/*Thread.sleep( 1000 );
				notifyMessage( SocketMsg.ADD_BOOKING, "1", "2", "27-08-2014", "27-08-2014", "Tommaso", "Catuogno", "0571/377750", "20" );
				/*Thread.sleep( 500 );
				notifyMessage( SocketMsg.ADD_TARIFF, "1", "15-04", "16-04", "10", "20" );
				Thread.sleep( 500 );
				notifyMessage( SocketMsg.ADD_TARIFF, "2", "01-12", "31-12", "10", "15" );
				Thread.sleep( 1500 );
				notifyMessage( SocketMsg.MODIFY_TARIFF, "2", "01-12", "30-12", "10", "15" );
				Thread.sleep( 1500 );
				notifyMessage( SocketMsg.DELETE_TARIFF, "2" );*/
				// ========================================== //

				wait_for_start();

				start_connection();

				if(!net.isConnected())
					continue;

				if(net.getChar() == SocketMsg.NO){
					notifyMessage( SocketMsg.NO, net.getMessage() );
					continue;
				}

				// avverte che il login e' andato a buon fine, e inserisce quanti dati sta per ricevere
				// il numero di cabine e sdraie con il relativo prezzo
				notifyMessage( SocketMsg.OK, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );

				// riceve tutti i dati degli ombrelloni
				close = false;
				while(!close){
					type = net.getChar();
					switch( type ){
						case( SocketMsg.ADD_PLACE ):
							// aggiunto per rendere piu' visibile l'aggiornamento
							Thread.sleep( 100 );
							IDPlace = net.getMessage();
							notifyMessage( type, IDPlace, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );

							break;

						case( SocketMsg.ADD_BOOKING ):
							message.add( IDPlace );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							cabine = net.getInt();
							message.add( cabine + "" );
							message.add( net.getMessage() );

							// riceve gli ID delle cabine prenotate
							for(int i = 0; i < cabine; i++)
								message.add( net.getMessage() );

							notifyMessage( type, (String[])message.toArray( contents ) );

							/*notifyMessage( type, IDPlace, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(),
										net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );*/

							message.clear();

							break;

						case( SocketMsg.ADD_TARIFF ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );

							break;

						case( SocketMsg.FINISH ):
							close = true;
							notifyMessage( type );

							break;
					}
				}

				// gestione degli aggiornamenti di rete dell'applicazione
				while(true){
					type = net.getChar();

					switch( type ){
						case( SocketMsg.ADD_BOOKING ):
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							cabine = net.getInt();
							message.add( cabine + "" );
							message.add( net.getMessage() );

							// riceve gli ID delle cabine prenotate
							for(int i = 0; i < cabine; i++)
								message.add( net.getMessage() );

							notifyMessage( type, (String[])message.toArray( contents ) );

							message.clear();

							/*notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(),
												 net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );*/

							break;

						case( SocketMsg.DELETE_BOOKING ):
							notifyMessage( type, net.getMessage(), net.getMessage() );

							break;

						case( SocketMsg.MODIFY_BOOKING ):
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							message.add( net.getMessage() );
							old_cabine = net.getInt();
							cabine = net.getInt();
							message.add( cabine + "" );
							message.add( net.getMessage() );
	
							// riceve gli ID di eventuali cabine aggiuntive
							if(cabine > old_cabine){
								for(int i = 0; i < cabine - old_cabine; i++)
									message.add( net.getMessage() );
							}
	
							notifyMessage( type, (String[])message.toArray( contents ) );

							message.clear();

							/*notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(),
									 			 net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );*/
							break;

						case( SocketMsg.OK ):
							// prenotazione/tariffa accettata
							if(BookingActivity.isOpen() || (RateActivity.isOpen() && RateActivity.type == RateActivity.CREATE)){
								if(BookingActivity.isOpen()){
									if(BookingActivity.type == BookingActivity.CREATE){
										message.add( net.getMessage() );

										cabine = net.getInt();
										// riceve gli ID delle cabine prenotate
										for(int i = 0; i < cabine; i++)
											message.add( net.getMessage() );

										notifyMessage( type, (String[])message.toArray( contents ) );

										message.clear();
									}
									else{
										old_cabine = net.getInt();
										cabine = net.getInt();

										if(cabine > old_cabine){
											// riceve gli ID delle cabine aggiuntive
											for(int i = 0; i < cabine - old_cabine; i++)
												message.add( net.getMessage() );

											notifyMessage( type, (String[])message.toArray( contents ) );

											message.clear();
										}
										else
											notifyMessage( type );
									}
								}
								else
									notifyMessage( type, net.getMessage() );
							}
							else
								notifyMessage( type );

							break;

						case( SocketMsg.NO ):
							// prenotazione/tariffa rifiutata
							notifyMessage( type, net.getMessage() );
							break;

						case( SocketMsg.ADD_PLACE ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );
							break;

						case( SocketMsg.MODIFY_PLACE ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage() );
							break;

						case( SocketMsg.DELETE_PLACE ):
							notifyMessage( type, net.getMessage() );
							break;

						case( SocketMsg.DELETE_ALL_PLACES ):
							notifyMessage( type );
							break;

						case( SocketMsg.ADD_TARIFF ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );
							break;

						case( SocketMsg.MODIFY_TARIFF ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );
							break;

						case( SocketMsg.DELETE_TARIFF ):
							notifyMessage( type, net.getMessage() );
							break;

						case( SocketMsg.MODIFY_DATA ):
							notifyMessage( type, net.getMessage(), net.getMessage(), net.getMessage(), net.getMessage() );
							break;

						case( SocketMsg.FINISH ):
							notifyMessage( type );
							break;

						case( SocketMsg.ERROR_SERVER ):
							notifyMessage( type );
							break;
					}
				}
			}
			catch( Exception e ){
				e.printStackTrace();

				net.closeConnection();

				notifyMessage( SocketMsg.SERVER_DOWN );
			}
		}
	}

	/** invia una lista di messaggi al thread UI per aggiornare la vista
	 * 
	 * @param type - tipo di messaggio
	 * @param messages - lista di messaggi
	*/
	private synchronized void notifyMessage( char type, String... messages )
	{
		Bundle b = new Bundle();
		b.putStringArray( type + "", messages );

		Message msg = Message.obtain( handler, type );
		msg.setData( b );
		handler.sendMessage( msg );
	}
}