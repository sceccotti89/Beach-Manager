
package stefano.ceccotti.beachmanager.engine;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.R;
import stefano.ceccotti.beachmanager.entities.Grid;
import stefano.ceccotti.beachmanager.entities.Ombrellone;
import stefano.ceccotti.beachmanager.entities.Rate;
import stefano.ceccotti.beachmanager.utils.Global;
import stefano.ceccotti.beachmanager.utils.SocketMsg;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;

public class BeachManager
{
	/* lista di ombrelloni */
	private ArrayList<Ombrellone> ombrelloni;
	/* lista di pacchetti delle tariffe */
	private ArrayList<Rate> packets;
	/* la vista dei bottoni */
	private TableLayout grid;
	/* la griglia dei bottoni */
	private Grid gr;
	/* coordinate X e Y massime */
	private int maxX = 0, maxY = 0;
	/* numero di colonne e di righe cambiate durante l'edit mode */
	private int changed_columns, changed_rows;
	/* coordinate X e Y massime durante l'edit */
	private int maxX_edit = 0, maxY_edit = 0;
	/* tipo di numerazione da eseguire */
	private int numeration;

	/* tipo di numerazione dei posti */
	private static final int MANUALLY = 1, SX_DX_TOP_BOTTOM = 2, SX_DX_BOTTOM_TOP = 3, DX_SX_TOP_BOTTOM = 4, DX_SX_BOTTOM_TOP = 5;
	/* risultati controllo stato dei posti selezionati */
	public static final int ADD_SINGLE = 0, ADD_MULTI = 1, DELETE_SINGLE = 2, DELETE_MULTI = 3, SINGLE = 4, MULTI = 5, NULL = 6;

	/** inizializza le strutture dati
	 * 
	 * @param activity - l'activity associata
	*/
	public BeachManager( Activity activity )
	{
		grid = (TableLayout) activity.findViewById( R.id.table_layout );
		grid.setPadding( (int)Global.startX, (int)Global.offsetY, 0, 0 );

		gr = (Grid) activity.findViewById( R.id.grid );

		ombrelloni = new ArrayList<Ombrellone>();

		packets = new ArrayList<Rate>();

		updateSettings( activity );
	}

	/** aggiorna le impostazioni
	 * 
	 * @param activity - l'activity associata
	*/
	public void updateSettings( Activity activity )
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences( activity );
		numeration = Integer.parseInt( sharedPrefs.getString( activity.getString( R.string.key_choose ), "1" ) );
		gr.setShow( sharedPrefs.getBoolean( activity.getString( R.string.key_grid ), true ) );
	}

	/** determina se la numerazione e' manuale
	 * 
	 * @return TRUE se la numerazione e' manuale, FALSE altrimenti
	*/
	public boolean isManually()
	{
		return numeration == MANUALLY;
	}

	/** ricostruisce la spiaggia
	 * 
	 * @param activity - l'activity associata
	*/
	public synchronized void re_build( Activity activity )
	{
		grid.removeAllViews();
		grid = (TableLayout) activity.findViewById( R.id.table_layout );
		grid.setPadding( (int)Global.startX, (int)Global.offsetY, 0, 0 );

		int size = ombrelloni.size(), maxY = 0;
		for(int i = 0; i < size; i++){
			Ombrellone o = ombrelloni.get( i );

			if(o.getY() > maxY){
				// aggiunge una nuova riga
				TableLayout.LayoutParams table_layout = new TableLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );
				TableRow.LayoutParams layout = new TableRow.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );

				TableRow row = new TableRow( activity );
				row.setLayoutParams( table_layout );

				ImageButton button = new ImageButton( activity );
				button.setLayoutParams( layout );
				row.addView( button );
				o.re_build( button );

				activity.registerForContextMenu( button );

				grid.addView( row );

				maxY++;
			}
			else{
				// aggiunge una nuova colonna
				TableRow.LayoutParams layout = new TableRow.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );
			    TableRow row = (TableRow) grid.getChildAt( maxY - 1 );

			    ImageButton button = new ImageButton( activity );
				button.setLayoutParams( layout );
				row.addView( button );
				o.re_build( button );

				activity.registerForContextMenu( button );
			}
		}

		gr = (Grid) activity.findViewById( R.id.grid );
		gr.setBounds( maxY, maxX );
	}

	/** aggiorna la posizione dopo che lo schermo e' cambiato */
	public void updatePadding()
	{
		grid.setPadding( (int)Global.startX, (int)Global.offsetY, 0, 0 );

		gr.update();
	}

	/** aggiorna la vista in base al nuovo periodo
	 * 
	 * @param date_from - data inizio
	 * @param date_to - data fine
	*/
	public synchronized void updateView( String date_from, String date_to )
	{
		int c_f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
		int c_f_month = Integer.parseInt( date_from.substring( 3, 5 ) );
		int c_f_year = Integer.parseInt( date_from.substring( 6 ) );

		int c_t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
		int c_t_month = Integer.parseInt( date_to.substring( 3, 5 ) );
		int c_t_year = Integer.parseInt( date_to.substring( 6 ) );

		for(int i = ombrelloni.size() - 1; i >= 0; i --)
			ombrelloni.get( i ).updateStatus( c_f_day, c_f_month, c_f_year, c_t_day, c_t_month, c_t_year );
	}

	/** restituisce l'ombrellone richiesto
	 * 
	 * @param IDPlace - ID della posizione richiesta
	 * 
	 * @return l'ombrellone, se presente, NULL altrimenti
	*/
	public synchronized Ombrellone getPlaceByID( int IDPlace )
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).getIDPlace() == IDPlace)
				return ombrelloni.get( i );
		}

		return null;
	}

	/** restituisce l'ombrellone alla posizione richiesta
	 * 
	 * @param position - posizione della postazione richiesta
	 * 
	 * @return l'ombrellone, se presente, NULL altrimenti
	*/
	public synchronized Ombrellone getPlaceByPosition( String position )
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).getPosition().equals( position ))
				return ombrelloni.get( i );
		}

		return null;
	}

	/** restituisce la posizione dell'ombrellone richiesto
	 * 
	 * @param IDPlace - ID dell'ombrellone
	*/
	public synchronized String getPosition( int IDPlace )
	{
		return getPlaceByID( IDPlace ).getPosition();
	}

	/** restituisce l'insieme di posti selezionati
	 * 
	 * @return l'insieme dei posti selezionati
	*/
	public synchronized ArrayList<Ombrellone> getSelectedPlaces()
	{
		int size = ombrelloni.size();
		ArrayList<Ombrellone> selected = new ArrayList<Ombrellone>( size );

		for(int i = 0; i < size; i++){
			if(ombrelloni.get( i ).isPressed())
				selected.add( ombrelloni.get( i ) );
		}

		return selected;
	}

	/** inserisce la modalità edit della spiaggia
	 * 
	 * @param activity - l'activity associata
	 * @param edit_mode - TRUE se e' in modalita' edit, FALSE altrimenti
	 * @param save - TRUE se e' stato premuto il tasto SAVE, FALSE altrimenti
	*/
	public synchronized void setEditMode( Activity activity, boolean edit_mode, boolean save )
	{
		if(edit_mode){
			EditText rows = (EditText) activity.findViewById( R.id.rows_edit );
			rows.setText( maxY + "" );

			EditText columns = (EditText) activity.findViewById( R.id.columns_edit );
			columns.setText( maxX + "" );

			maxX_edit = maxX;
			maxY_edit = maxY;

			changed_columns = 0;
			changed_rows = 0;

			for(int i = ombrelloni.size() - 1; i >= 0; i--)
				ombrelloni.get( i ).setEditMode( true );
		}
		else{
			maxX = maxX - changed_columns;
			maxY = maxY - changed_rows;

			int size = ombrelloni.size();
			if(size > 0){
				if(save){
					if(maxX_edit == 0 || maxY_edit == 0){
						// controlla se deve inviare il messaggio
						boolean send_message = false;
						for(int i = 0; i < size; i++){
							if(ombrelloni.get( i ).getStatus() != Ombrellone.EMPTY){
								send_message = true;
								break;
							}
						}

						deleteAllPlaces();

						if(send_message)
							Global.net.sendMessage( SocketMsg.DELETE_ALL_PLACES + "" );

						maxX = maxX_edit = 0;
						maxY = maxY_edit = 0;
					}
					else{
						int status, X, Y;
						Ombrellone ombrellone;
						String name = "";
						int final_size = maxX_edit * maxY_edit;

						// cancella eventuali colonne eliminate
						for(int i = maxX - 1; i >= maxX_edit; i--){
							for(int j = maxY - 1; j >= 0; j--){
								Global.net.sendMessage( SocketMsg.DELETE_PLACE + "", ombrelloni.get( j * maxX + i ).getIDPlace() + "" );
								ombrellone = ombrelloni.remove( j * maxX + i );
								((TableRow) grid.getChildAt( ombrellone.getY() - 1 )).removeViewAt( ombrellone.getX() - 1 );
							}

							maxX--;
						}

						// cancella eventuali righe eliminate
						for(int i = maxY - 1; i >= maxY_edit; i--){
							for(int j = maxX - 1; j >= 0; j--){
								ombrellone = ombrelloni.remove( i * maxX + j );
								Global.net.sendMessage( SocketMsg.DELETE_PLACE + "", ombrellone.getIDPlace() + "" );
								X = ombrellone.getX();
								Y = ombrellone.getY();
								((TableRow) grid.getChildAt( Y - 1 )).removeViewAt( X - 1 );
								if(((TableRow) grid.getChildAt( Y - 1 )).getChildCount() == 0)
									grid.removeViewAt( Y - 1 );
							}

							maxY--;
						}

						// assegna la numerazioni agli ombrelloni
						if(numeration != MANUALLY){
							int remove = 0, k = 0, y = -1;
							for(int i = 0; i < final_size; i++){
								ombrellone = ombrelloni.get( i );
								status = ombrellone.getEditStatus();
								if(ombrellone.isNameNumber() &&
								   (status == Ombrellone.OLD_INSERT ||
								   status == Ombrellone.NEW_INSERT ||
								   (status == Ombrellone.OK && ombrellone.getStatus() != Ombrellone.NULL))){
									switch( numeration ){
										case( SX_DX_TOP_BOTTOM ):
											name = getPlaceNumber( k++, 0, final_size );
											break;

										case( SX_DX_BOTTOM_TOP ):
											if(ombrellone.getY() == y)
												name = getPlaceNumber( i, 1, final_size );
											else
												name = getPlaceNumber( i, 0, final_size );
											break;

										case( DX_SX_TOP_BOTTOM ):
											if(ombrellone.getY() == y)
												name = getPlaceNumber( i, remove - 1, final_size );
											else
												name = getPlaceNumber( i, remove, final_size );
											break;

										case( DX_SX_BOTTOM_TOP ):
											name = getPlaceNumber( i, 0, final_size );
											break;
									}

									ombrellone.setTmpName( name );
								}
								else{
									switch( numeration ){
										case( SX_DX_BOTTOM_TOP ):
											y = ombrellone.getY();

											for(int j = i - (i % maxX_edit) - 1; j >= 0; j--){
												if(ombrelloni.get( j ).isTmpNameNumber()){
													name = ombrelloni.get( j ).getTmpName();
													ombrelloni.get( j ).setTmpName( (Integer.parseInt( name ) - 1) + "" );
												}
											}

											break;

										case( SX_DX_TOP_BOTTOM ):
											break;

										case( DX_SX_TOP_BOTTOM ):
											y = ombrellone.getY();

											for(int j = 1; j <= i % maxX_edit && i - j >= 0; j++){
												if(ombrelloni.get( i - j ).isTmpNameNumber()){
													name = ombrelloni.get( i - j ).getTmpName();
													ombrelloni.get( i - j ).setTmpName( (Integer.parseInt( name ) - 1) + "" );
												}
											}
	
											remove++;

											break;

										case( DX_SX_BOTTOM_TOP ):
											for(int j = i - 1; j >= 0; j--){
												if(ombrelloni.get( j ).isTmpNameNumber()){
													name = ombrelloni.get( j ).getTmpName();
													ombrelloni.get( j ).setTmpName( (Integer.parseInt( name ) - 1) + "" );
												}
											}

											break;
									}
								}
							}
						}

						for(int i = final_size - 1; i >= 0; i--){
							ombrellone = ombrelloni.get( i );
							status = ombrellone.getEditStatus();
							X = ombrellone.getX();
							Y = ombrellone.getY();

							switch( status ){
								case( Ombrellone.NEW_INSERT ):
									Global.net.sendMessage( SocketMsg.ADD_PLACE + "", X + "", Y + "",
															ombrellone.getTmpName(), ombrellone.getPrice() + "" );
									break;

								case( Ombrellone.OLD_INSERT ):
									Global.net.sendMessage( SocketMsg.ADD_PLACE + "", X + "", Y + "",
															ombrellone.getTmpName(), ombrellone.getPrice() + "" );
									break;

								case( Ombrellone.OLD_DELETE ):
									Global.net.sendMessage( SocketMsg.DELETE_PLACE + "", ombrellone.getIDPlace() + "" );
									deletePlace( ombrellone.getIDPlace() );

									break;

								case( Ombrellone.OK ):
									if(ombrellone.getIDPlace() > 0 && !ombrellone.checkName())
										Global.net.sendMessage( SocketMsg.MODIFY_PLACE + "", ombrellone.getIDPlace() + "",
																ombrellone.getName(), ombrellone.getPrice() + "" );

									break;
							}

							if(status == Ombrellone.EMPTY || status == Ombrellone.NEW_INSERT || status == Ombrellone.NEW_DELETE){
								ombrelloni.remove( i );
								((TableRow) grid.getChildAt( Y - 1 )).removeViewAt( X - 1 );
								if(((TableRow) grid.getChildAt( Y - 1 )).getChildCount() == 0)
									grid.removeViewAt( Y - 1 );
							}

							ombrellone.setEditMode( false );
						}

						// notifica il server che i dati sono stati tutti inviati
						Global.net.sendMessage( SocketMsg.FINISH + "" );
					}
				}
				else{
					// ripristina lo stato iniziale
					for(int i = maxX_edit; i < maxX; i++)
						grid.setColumnCollapsed( i, false );

					for(int i = maxY_edit; i < maxY; i++)
						grid.getChildAt( i ).setVisibility( TableRow.VISIBLE );

					int edit_status, X, Y;
					Ombrellone ombrellone;
					for(int i = ombrelloni.size() - 1; i >= 0; i--){
						ombrellone = ombrelloni.get( i );
						edit_status = ombrellone.getEditStatus();
						if(edit_status != Ombrellone.OK && edit_status != Ombrellone.OLD_INSERT && edit_status != Ombrellone.OLD_DELETE){
							X = ombrellone.getX();
							Y = ombrellone.getY();
							ombrelloni.remove( i );
							((TableRow) grid.getChildAt( Y - 1 )).removeViewAt( X - 1 );
							if(((TableRow) grid.getChildAt( Y - 1 )).getChildCount() == 0)
								grid.removeViewAt( Y - 1 );
						}

						ombrellone.setEditMode( false );
					}
				}

				gr.setBounds( maxY, maxX );
			}
		}
	}

	/** restituisce la numerazione dell'ombrellone
	 * 
	 * @param index - indice dell'ombrellone da analizzare
	 * @param remove - quanti valori togliere alla numerazione
	 * @param size - numero totale, finale, di ombrelloni
	 * 
	 * @return il numero associato alla posizione
	*/
	private String getPlaceNumber( int index, int remove, int size )
	{
		switch( numeration ){
			case( SX_DX_TOP_BOTTOM ):
				return (index + 1 - remove) + "";

			case( SX_DX_BOTTOM_TOP ):
				int X = ombrelloni.get( index ).getX();
				int Y = ombrelloni.get( index ).getY();

				return ((maxY_edit - Y) + 1) * maxX_edit - (maxX_edit - X) - remove + "";

			case( DX_SX_TOP_BOTTOM ):
				X = ombrelloni.get( index ).getX();
				Y = ombrelloni.get( index ).getY();

				return (Y - 1) * maxX_edit + (maxX_edit - X) + 1 - remove + "";

			case( DX_SX_BOTTOM_TOP ):
				return size - index - remove + "";
		}

		return "0";
	}

	/** aggiunge o rimuove temporaneamente righe e colonne
	 * 
	 * @param activity - l'activity associata
	 * @param X - massima coordinata X 
	 * @param Y - massima coordinata Y
	*/
	public synchronized void update_edit_view( Activity activity, int X, int Y )
	{
		if((maxX_edit == 0 || maxY_edit == 0) && (Y == 0 || X == 0))
			return;

		if(Y > maxY_edit){
			// aggiunge righe
			int n = Math.min( Y, maxY );
			for(int i = maxY_edit; i < n; i++)
				grid.getChildAt( i ).setVisibility( TableRow.VISIBLE );

			maxY_edit = Y;

			if(Y > maxY){
				changed_rows = changed_rows + (Y - maxY);

				addPlace( activity, null, null, 0, Y, maxX, null, 0 );
			}
		}

		if(Y < maxY_edit){
			// toglie righe
			for(int i = maxY_edit - 1; i >= Y; i--)
				grid.getChildAt( i ).setVisibility( TableRow.GONE );

			maxY_edit = Y;
		}

		if(X > maxX_edit){
			// aggiunge colonne
			int n = Math.min( X, maxX );
			for(int i = maxX_edit; i < n; i++)
				grid.setColumnCollapsed( i, false );

			maxX_edit = X;

			if(X > maxX){
				changed_columns = changed_columns + (X - maxX);

				addPlace( activity, null, null, 0, maxY, X, null, 0 );
			}
		}

		if(X < maxX_edit){
			// toglie colonne
			for(int i = X; i < maxX_edit; i++)
				grid.setColumnCollapsed( i, true );

			maxX_edit = X;
		}

		gr.setBounds( Y, X );
	}

	/** restituisce lo stato dei posti selezionati
	 * 
	 * @param status - lo stato dell'ombrellone selezionato
	 * 
	 * @return lo stato
	*/
	public synchronized int getPlacesStatus( int status )
	{
		int count = 0, stato;
		boolean to_check = true;

		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).isPressed()){
				count++;
				if(to_check){
					stato = ombrelloni.get( i ).getStatus();
					if((count > 1 && stato == Ombrellone.OCCUPIED) ||
					   ((status == Ombrellone.NULL || status == Ombrellone.FREE) && stato == Ombrellone.OCCUPIED) ||
					   (status == Ombrellone.OCCUPIED && (stato == Ombrellone.NULL || stato == Ombrellone.FREE)))
						to_check = false;
				}
			}
		}

		if(to_check){
			if(count == 1)
				return SINGLE;
			else
				return MULTI;
		}
		else
			return NULL;
	}

	/** restituisce lo stato dei posti selezionati durante l'edit della spiaggia
	 * 
	 * @param edit_status - lo stato da controllare
	 * @param status - lo stato dell'ombrellone premuto
	 * 
	 * @return lo stato degli ombrelloni selezionati
	*/
	public synchronized int getPlacesEditStatus( int edit_status, int status )
	{
		boolean to_check = true;
		int count = 0, stato, edit_stato;

		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).isPressed()){
				count++;
				if(to_check){
					stato = ombrelloni.get( i ).getStatus();
					edit_stato = ombrelloni.get( i ).getEditStatus();
					if((((edit_status == Ombrellone.OK && status == Ombrellone.NULL) || edit_status == Ombrellone.EMPTY ||
							edit_status == Ombrellone.NEW_DELETE || edit_status == Ombrellone.OLD_DELETE) &&
						((edit_stato == Ombrellone.OK && stato != Ombrellone.NULL) || edit_stato == Ombrellone.NEW_INSERT ||
							edit_stato == Ombrellone.OLD_INSERT)) ||
					   (((edit_status == Ombrellone.OK && status != Ombrellone.NULL) || edit_status == Ombrellone.NEW_INSERT ||
					   		edit_status == Ombrellone.OLD_INSERT) &&
						((edit_stato == Ombrellone.OK && stato == Ombrellone.NULL) || edit_stato == Ombrellone.EMPTY ||
							edit_stato == Ombrellone.NEW_DELETE || edit_stato == Ombrellone.OLD_DELETE)))
						to_check = false;
				}
			}
		}

		if(to_check){
			if((edit_status == Ombrellone.OK && status == Ombrellone.NULL) || edit_status == Ombrellone.EMPTY ||
					edit_status == Ombrellone.NEW_DELETE || edit_status == Ombrellone.OLD_DELETE){
				if(count == 1)
					return ADD_SINGLE;
				else
					return ADD_MULTI;
			}
			else{
				if(count == 1)
					return DELETE_SINGLE;
				else
					return DELETE_MULTI;
			}
		}
		else
			return NULL;
	}

	/** modifica lo stato dei posti
	 * 
	 * @param pressed - TRUE se devono essere selezionati, FALSE altrimenti
	*/
	public synchronized void setPressed( boolean pressed )
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--)
			ombrelloni.get( i ).setPressed( pressed );
	}

	/** aggiunge un posto
	 * 
	 * @param activity - l'activity associata
	 * @param from - data di inizio periodo
	 * @param to - data di fine periodo
	 * @param IDPlace - ID del posto da aggiungere
	 * @param Y - la coordinata Y
	 * @param X - la coordinata X
	 * @param name - nome dell'ombrellone
	 * @param price - valore dell'ombrellone
	*/
	public synchronized void addPlace( Activity activity, String from, String to, int IDPlace, int Y, int X, String name, float price )
	{
		if(Y > maxY){
			int rows = Y - maxY;

			// carica i layout della tabella e delle righe
			TableLayout.LayoutParams table_layout = new TableLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );
			TableRow.LayoutParams row_layout = new TableRow.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );
			ImageButton button;

			for(int i = 0; i < rows; i++){
				TableRow row = new TableRow( activity );
				row.setLayoutParams( table_layout );

				for(int j = 0; j < maxX; j++){
					button = new ImageButton( activity );
					button.setLayoutParams( row_layout );
					row.addView( button );

					button.setId( -1 );

					Ombrellone o = new Ombrellone( button, j + 1, maxY + i + 1 );
					if(IDPlace == 0)
						o.setEditStatus( Ombrellone.EMPTY );

					ombrelloni.add( o );

					activity.registerForContextMenu( button );
				}

				grid.addView( row );
			}

			maxY = Y;

			gr.setBounds( Y, maxX );
		}

		if(X > maxX){
			int columns = X - maxX;

			TableRow.LayoutParams layout = new TableRow.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1 );
			ImageButton button;

			for(int i = 0; i < maxY; i++){
			    TableRow row = (TableRow) grid.getChildAt( i );

				for(int j = 0; j < columns; j++){
					button = new ImageButton( activity );
					button.setLayoutParams( layout );
					row.addView( button );

					button.setId( -1 );
					activity.registerForContextMenu( button );

					Ombrellone o = new Ombrellone( button, maxX + j + 1, i + 1 );
					if(IDPlace == 0)
						o.setEditStatus( Ombrellone.EMPTY );

					ombrelloni.add( i * X + maxX + j, o );
				}
			}

			gr.setBounds( maxY, X );

			maxX = X;
		}

		if(IDPlace > 0)
			ombrelloni.get( (Y - 1) * maxX + X - 1 ).setVisibility( true, IDPlace, name, price );
	}

	/** marca i posti per essere aggiunti */
	public synchronized void setAddPlaces()
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).isPressed())
				ombrelloni.get( i ).setEditStatus( Ombrellone.INSERT );
		}
	}

	/** modifica un posto
	 * 
	 * @param IDPlace - ID del posto da modificare
	 * @param name - il nuovo nome
	 * @param price - il nuovo prezzo
	*/
	public synchronized void modifyPlace( int IDPlace, String name, float price )
	{
		Ombrellone ombrellone = getPlaceByID( IDPlace );
		if(ombrellone != null)
			ombrellone.modifyValues( name, price );
	}

	/** modifica il prezzo dei posti selezionati
	 * 
	 * @param price - il nuovo prezzo
	*/
	public synchronized void modifyPricePlaces( float price )
	{
		Ombrellone ombrellone;
		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			ombrellone = ombrelloni.get( i );
			if(ombrellone.isPressed()){
				ombrellone.setPrice( price );
				Global.net.sendMessage( SocketMsg.MODIFY_PLACE + "", ombrellone.getIDPlace() + "", ombrellone.getName(), price + "" );
				ombrellone.setPressed( false );
			}
		}
	}

	/** cancella un posto
	 * 
	 * @param IDPlace - ID del posto da cancellare
	*/
	public synchronized void deletePlace( int IDPlace )
	{
		Ombrellone ombrellone = getPlaceByID( IDPlace );
		if(ombrellone == null)
			return;

		ombrellone.setVisibility( false, -1, null, 0 );

		// verifica se sistemare la struttura in modo da usare meno spazio
		int X = ombrellone.getX();
		int Y = ombrellone.getY();

		if(Y == maxY){
			while(maxY > 0){
				// controlla prima se tutta la riga e' inutilizzata
				int i, size = ombrelloni.size();
				for(i = 0; i < maxX; i++){
					if(ombrelloni.get( size - i - 1 ).getButton().getVisibility() == ImageButton.VISIBLE)
						break;
				}

				if(i < maxX)
					break;
				else{
					for(i = 0; i < maxX; i++)
						ombrelloni.remove( size - i - 1 );

					grid.removeViewAt( maxY - 1 );

					gr.setBounds( --maxY, maxX );
				}
			}
		}

		if(X == maxX){
			while(maxX > 0){
				// controlla prima se tutta la colonna e' inutilizzata
				int i;
				for(i = 0; i < maxY; i++){
					if(ombrelloni.get( (i + 1) * maxX - 1 ).getButton().getVisibility() == ImageButton.VISIBLE)
						break;
				}

				if(i < maxY)
					break;
				else{
					for(i = 0; i < maxY; i++){
						ombrelloni.remove( (maxY - i) * maxX - 1 );
					    ((TableRow) grid.getChildAt( i )).removeViewAt( maxX - 1 );
					}

					gr.setBounds( maxY, --maxX );
				}
			}
		}
	}

	/** marca i posti per essere cancellati */
	public synchronized void deletePlaces()
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).isPressed())
				ombrelloni.get( i ).setEditStatus( Ombrellone.DELETE );
		}
	}

	/** cancella tutti i posti */
	public synchronized void deleteAllPlaces()
	{
		ombrelloni.clear();

		for(int i = 0; i < maxY; i++){
			for(int j = maxX - 1; j >= 0; j--)
				((TableRow) grid.getChildAt( i )).removeViewAt( j );
		}

		grid.removeAllViews();

		maxX = maxY = 0;
		maxX_edit = maxY_edit = 0;

		gr.setBounds( 0, 0 );
	}

	/** aggiunge una prenotazione
	 * 
	 * @param IDPlace - ID del posto a cui aggiungere la prenotazione
	 * @param IDPrenotazione - ID associato alla prenotazione
	 * @param date_from - data di inizio prenotazione
	 * @param date_to - data di scadenza prenotazione
	 * @param name - nome dell'utente che ha prenotato
	 * @param surname - cognome dell'utente che ha prenotato
	 * @param phone - numero di telefono dell'utente che ha prenotato
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	 * @param current_date_from - data di inizio periodo selezionata
	 * @param current_date_to - data di fine periodo selezionata
	 * @param IDCabins - lista degli ID delle cabine prenotate
	*/
	public synchronized void addBooking( int IDPlace, int IDPrenotazione, String date_from, String date_to, String name,
										 String surname, String phone, int cabins, int deckchairs,
										 String current_date_from, String current_date_to, String IDCabins[] ){
		Ombrellone ombrellone = getPlaceByID( IDPlace );
		if(ombrellone != null)
			ombrellone.addBooking( IDPrenotazione, date_from, date_to, name, surname, phone,
									cabins, deckchairs, compute_price( date_from, date_to, cabins, deckchairs ),
									current_date_from, current_date_to, IDCabins );
	}

	/** modifica una prenotazione
	 * 
	 * @param IDPlace - ID del posto a cui modificare la prenotazione
	 * @param IDPrenotazione - ID della prenotazione da modificare
	 * @param date_from - inizio prenotazione
	 * @param date_to - scadenza prenotazione
	 * @param name - nome dell'utente della prenotazione
	 * @param surname - cognome dell'utente della prenotazione
	 * @param phone - numero di telefono dell'utente della prenotazione
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	 * @param current_date_from - data di inizio periodo selezionata
	 * @param current_date_to - data di fine periodo selezionata
	 * @param IDCabins - lista degli ID delle cabine prenotate
	*/
	public synchronized void modifyBooking( int IDPlace, int IDPrenotazione, String date_from, String date_to, String name, String surname,
											String phone, int cabins, int deckchairs,
											String current_date_from, String current_date_to, String IDCabins[] )
	{
		Ombrellone ombrellone = getPlaceByID( IDPlace );
		if(ombrellone != null)
			ombrellone.modifyBooking( IDPrenotazione, date_from, date_to, name, surname, phone,
									  cabins, deckchairs, compute_price( date_from, date_to, cabins, deckchairs ),
									  current_date_from, current_date_to, IDCabins );
	}

	/** cancella una prenotazione
	 * 
	 * @param IDPlace - ID del posto a cui cancellare la prenotazione
	 * @param IDPrenotazione - ID della prenotazione da cancellare
	 * @param date_from - data di inizio periodo selezionata
	 * @param date_to - data di fine periodo selezionata
	*/
	public synchronized void deleteBooking( int IDPlace, int IDPrenotazione, String date_from, String date_to )
	{
		Ombrellone ombrellone = getPlaceByID( IDPlace );
		if(ombrellone != null)
			ombrellone.deleteBooking( IDPrenotazione, date_from, date_to );
	}

	/** calcola il prezzo del periodo richiesto
	 * 
	 * @param date_from - data di inizio periodo
	 * @param date_to - data di fine periodo
	 * @param cabine - numero di cabine selezionate
	 * @param sdraie - numero di sdraie selezionate
	 * 
	 * @return il prezzo
	*/
	public synchronized float compute_price( String date_from, String date_to, int cabine, int sdraie )
	{
		float price = cabine * Global.prezzo_cabina + sdraie * Global.prezzo_sdraia;
		String from, to;

		for(int i = ombrelloni.size() - 1; i >= 0; i--){
			if(ombrelloni.get( i ).isPressed()){
				//TODO int value = ombrelloni.get( i ).getPrice();
				int start = -1;

				int size = packets.size();
				for(int j = 0; j < size; j++){
					from = packets.get( j ).getDateFrom();
					to = packets.get( j ).getDateTo();

					if(start == -1){
						if(Global.compare_dates( date_from, from ) <= 0 &&
						   Global.compare_dates( date_from, to ) >= 0)
							start = j;
					}

					// TODO devo farmi dare la formula da tomma per il calcolo del costo
					// controlla se la data e' finita in una tariffa
					if(Global.compare_dates( date_to, from ) <= 0 &&
					   Global.compare_dates( date_to, to ) >= 0){
						System.out.println( "INDICE: " + j );
						// TODO aggiunge il costo dell'attuale tariffa
						// TODO devo calcolare i giorni occupati, cioè parto da quella selezionata e arrivo a quella di partenza
						// TODO stando attento alla durata dei vari mesi (questa penso di sbrigarmela velocemente)
						

						break;
					}
					else{
						// TODO aggiunge il costo dell'intera tariffa
						//price = price + ;
					}
				}
			}
		}

		return price;
	}

	/** restituisce la lista dei pacchetti delle tariffe
	 * 
	 * @return la lista dei pacchetti delle tariffe
	*/
	public synchronized ArrayList<Rate> getRatesList()
	{
		return packets;
	}

	/** restituisce lo stato della tariffa in base al periodo selezionato
	 * 
	 * @param IDRate - ID della tariffa da non considerare
	 * @param date_from - data inizio periodo
	 * @param date_to - data fine periodo
	*/
	public synchronized int check_rate_status( int IDRate, String date_from, String date_to )
	{
		int c_f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
		int c_f_month = Integer.parseInt( date_from.substring( 3, 5 ) );

		int c_t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
		int c_t_month = Integer.parseInt( date_to.substring( 3, 5 ) );

		for(int i = packets.size() - 1; i >= 0; i--){
			if(packets.get( i ).getIDRate() != IDRate){
				String from = packets.get( i ).getDateFrom();
				String to = packets.get( i ).getDateTo();

				int l_f_day = Integer.parseInt( from.substring( 0, 2 ) );
				int l_f_month = Integer.parseInt( from.substring( 3, 5 ) );

				int l_t_day = Integer.parseInt( to.substring( 0, 2 ) );
				int l_t_month = Integer.parseInt( to.substring( 3, 5 ) );

				if((((l_f_month > c_f_month) ||
					(l_f_month == c_f_month && l_f_day >= c_f_day)) &&
				   ((l_f_month < c_t_month) ||
					(l_f_month == c_t_month && l_f_day <= c_t_day))) ||
				   (((l_t_month > c_f_month) ||
					(l_t_month == c_f_month && l_t_day >= c_f_day)) &&
				   ((l_t_month < c_t_month) ||
					(l_t_month == c_t_month && l_t_day <= c_t_day))) ||
				   (((l_f_month < c_f_month) ||
					(l_f_month == c_f_month && l_f_day <= c_f_day)) &&
				   ((l_t_month > c_t_month) ||
					(l_t_month == c_t_month && l_t_day >= c_t_day))))
						return Rate.OCCUPIED;
			}
		}

		return Rate.FREE;
	}

	/** restituisce la tariffa corrispondente all'indice richiesto
	 * 
	 * @param IDRate - ID della tariffa
	 * 
	 * @return la tariffa, se c'e', NULL altrimenti
	*/
	public synchronized Rate getRateByID( int IDRate )
	{
		for(int i = packets.size() - 1; i >= 0; i--){
			if(packets.get( i ).getIDRate() == IDRate)
				return packets.get( i );
		}

		return null;
	}

	/** aggiunge una tariffa
	 * 
	 * @param IDRate - ID della tariffa
	 * @param date_from - data inizio periodo
	 * @param date_to - data fine periodo
	 * @param daily_price - prezzo giornaliero
	 * @param weekly_price - prezzo settimanale
	 * 
	 * @return l'indice della posizione della tariffa
	*/
	public synchronized int addRate( int IDRate, String date_from, String date_to, float daily_price, float weekly_price )
	{
		// inserisce l'oggetto nella lista in maniera ordinata
		int i, size = packets.size();
		for(i = 0; i < size; i++){
			if(Global.compare_dates( date_to, packets.get( i ).getDateFrom() ) >= 0)
				break;
		}

		packets.add( i, new Rate( IDRate, date_from, date_to, daily_price, weekly_price ) );

		return i + 1;
	}

	/** modifica una tariffa
	 * 
	 * @param IDRate - ID della tariffa
	 * @param date_from - data inizio periodo
	 * @param date_to - data fine periodo
	 * @param daily_price - prezzo giornaliero
	 * @param weekly_price - prezzo settimanale
	 * 
	 * @return l'indice della posizione della tariffa
	*/
	public synchronized int modifyRate( int IDRate, String date_from, String date_to, float daily_price, float weekly_price )
	{
		for(int i = packets.size() - 1; i >= 0; i--){
			if(packets.get( i ).getIDRate() == IDRate){
				packets.get( i ).modifyRate( date_from, date_to, daily_price, weekly_price );
				return i + 1;
			}
		}

		return -1;
	}

	/** cancella una tariffa
	 * 
	 * @param IDRate - ID della tariffa
	*/
	public synchronized int deleteRate( int IDRate )
	{
		for(int i = packets.size() - 1; i >= 0; i--){
			if(packets.get( i ).getIDRate() == IDRate){
				packets.remove( i );
				return i + 1;
			}
		}

		return -1;
	}

	/** cancella tutte le tariffe */
	public synchronized void deleteAllRates()
	{
		packets.clear();
	}
}