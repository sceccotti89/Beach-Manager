
package stefano.ceccotti.beachmanager;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.entities.Booking;
import stefano.ceccotti.beachmanager.entities.Ombrellone;
import stefano.ceccotti.beachmanager.utils.ErrorMessage;
import stefano.ceccotti.beachmanager.utils.Global;
import stefano.ceccotti.beachmanager.utils.SocketMsg;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BookingActivity extends ActionBarActivity implements OnClickListener
{
	/* l'ombrellone */
	public static ArrayList<Ombrellone> ombrelloni;
	/* determina il tipo di prenotazione */
	public static int type;
	/* nome, cognome, telefono, numero di cabine e sdraie della prenotazione */
	private static String name, surname, phone, cabine, sdraie;
	/* periodo selezionato nella schermata iniziale (non modificare) */
	public static String current_date_from, current_date_to;
	/* data di inizio e fine periodo di prenotazione */
	public static String date_from, date_to;
	/* ID della prenotazione selezionata */
	public static int IDPrenotazione;
	/* dialog per le notifiche */
	private static Dialog notify;
	/* indica il prossimo ombrellone da aggiornare */
	private static int next, next_place;
	/* il toast per le segnalazioni */
	private static Toast toast;
	/* contiene il valore del numero di cabine e sdraie */
	private EditText value;
	/* numero di cabine e sdraie selezionate */
	private static int n_cabins, n_deckchairs;
	/* determina se l'activity e' attiva */
	private static boolean isOpen = false;
	/* determina se e' aperta la sezione delle cabine */
	private boolean isCabinOpen = false;
	/* lista di prenotazioni rifiutate */
	private static ArrayList<String> texts;
	/* dialog per gli avvisi */
	private static AlertDialog dialog;
	/* contiene il testo del dialog */
	private static TextView text;
	
	/* questa activity */
	private static Activity activity;

	/* il contesto dell'activity */
	private final Context context = this;
	/* tipo di prenotazione */
	public static final int MODIFY = 0, CREATE = 1;
	/* chavi per salvare lo stato */
	private static final String K_NAME = "BOOK_NAME", K_SURNAME = "BOOK_SURNAME", K_PHONE = "BOOK_PHONE", K_STATE = "BOOK_STATE",
								K_DATE_FROM = "BOOK_DATE_FROM", K_DATE_TO = "BOOK_DATE_TO", K_CABINS = "BOOK_CABINS",
								K_DECKCHAIRS = "BOOK_DECKCHAIRS", K_PRICE = "BOOK_PRICE", K_CHECK = "BOOK_CHECK";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.booking_activity );

		activity = BookingActivity.this;

		next = 0;
		n_cabins = 0;
		n_deckchairs = 0;
		next_place = 0;

		texts = new ArrayList<String>();

		EditText cabins = (EditText) findViewById( R.id.cabin_edit );
		cabins.setOnClickListener( this );
		EditText deckchairs = (EditText) findViewById( R.id.deckchair_edit );
		deckchairs.setOnClickListener( this );

		setOpen( true );

		Global.isInBackground = false;

		TextView text = (TextView) findViewById( R.id.title_text );
		EditText from = (EditText) findViewById( R.id.date_from_edit );
		from.setTextSize( 19 );
		from.setTextScaleX( Global.ratioW );

		EditText to = (EditText) findViewById( R.id.date_to_edit );
		to.setTextSize( 19 );
		to.setTextScaleX( Global.ratioW );

		if(type == MODIFY)
			text.setText( R.string.modify_booking );
		else{
			text.setText( R.string.new_booking );
			IDPrenotazione = -1;

			current_date_from = date_from;
			current_date_to = date_to;

			cabine = sdraie = "0";

			cabins.setText( "0" );
			deckchairs.setText( "0" );
		}

		from.setText( date_from );
		to.setText( date_to );

		updatePlaceStatus( true, 0, false, false, false, 0 );

		from.setOnClickListener( this );
		to.setOnClickListener( this );

		Button ok = (Button) findViewById( R.id.okButton );
		ok.setOnClickListener( this );

		Button cancel = (Button) findViewById( R.id.cancelButton );
		cancel.setOnClickListener( this );

		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable( new ColorDrawable( Color.parseColor( "#0099FF" ) ) );
		bar.setTitle( Html.fromHtml( "<font color=\"white\">" + getString( R.string.app_name ) + "</font>" ) );

		if(savedInstanceState != null){
			// ripristina lo stato
			((EditText) findViewById( R.id.name_edit )).setText( savedInstanceState.getString( K_NAME ) );
			((EditText) findViewById( R.id.surname_edit )).setText( savedInstanceState.getString( K_SURNAME ) );
			((EditText) findViewById( R.id.telephone_edit )).setText( savedInstanceState.getString( K_PHONE ) );
			from.setText( savedInstanceState.getString( K_DATE_FROM ) );
			to.setText( savedInstanceState.getString( K_DATE_TO ) );

			String sts = savedInstanceState.getString( K_STATE );
			TextView status = (TextView) findViewById( R.id.status_value_text );
			status.setText( sts );
			if(sts.equals( getString( R.string.free ) ))
				status.setTextColor( Color.GREEN );
			else
				status.setTextColor( Color.RED );

			((EditText) findViewById( R.id.cabin_edit )).setText( savedInstanceState.getString( K_CABINS ) );
			((EditText) findViewById( R.id.deckchair_edit )).setText( savedInstanceState.getString( K_DECKCHAIRS ) );
			((EditText) findViewById( R.id.price_edit )).setText( savedInstanceState.getString( K_PRICE ) );
		}

		getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged( newConfig );

		Global.init( null, true );

		Global.bm.updatePadding();

		EditText from = (EditText) findViewById( R.id.date_from_edit );
		from.setTextSize( 19 );
		from.setTextScaleX( Global.ratioW );

		EditText to = (EditText) findViewById( R.id.date_to_edit );
		to.setTextSize( 19 );
		to.setTextScaleX( Global.ratioW );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_booking, menu );

		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() ){
			case( R.id.menu_settings ):
				// apre il menu' delle impostazioni
				Intent i = new Intent( this, PrefActivity.class );
				startActivity( i );

				break;
		}

		return true;
	}

	@Override
	protected void onPause()
	{
		Global.isInBackground = true;

		super.onPause();
	}

	@Override
	protected void onResume()
	{
		Global.isInBackground = false;

		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		setOpen( false );

		ombrelloni.clear();

		dialog = null;
		notify = null;
	
		super.onDestroy();
	}

	/** invia un messaggio in presenza di prenotazioni multiple
	 *
	 * @param date_from - data di inizio periodo
	 * @param date_to - data di fine periodo
	 * @param name - nome dell'utente
	 * @param surname - cognome dell'utente
	 * @param phone - numero di telefono dell'utente
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	*/
	private void sendMultiBookings( String date_from, String date_to, String name, String surname, String phone, String cabins, String deckchairs )
	{
		int i = 0;

		if(name.equals( "" )) name = "-";
		if(surname.equals( "" )) surname = "-";
		if(phone.equals( "" )) phone = "-";

		// se le cabine o sdraie sono più di uno le aggiunge solo alla prima, alle altre mette 0
		if(!cabins.equals( "0" ) || !deckchairs.equals( "0" ))
			Global.net.sendMessage( SocketMsg.ADD_BOOKING + "", ombrelloni.get( i++ ).getIDPlace() + "",
					date_from, date_to, name, surname, phone, cabins, deckchairs );

		int size = ombrelloni.size();
		for( ; i < size; i++)
			Global.net.sendMessage( SocketMsg.ADD_BOOKING + "", ombrelloni.get( i ).getIDPlace() + "",
									date_from, date_to, name, surname, phone, "0", "0" );
	}

	@Override
	public void onClick( View v )
	{
		switch( v.getId() ){
			case( R.id.okButton ):
				if(type == CREATE){
					final String e_name = ((EditText) findViewById( R.id.name_edit )).getText().toString();
					final String e_surname = ((EditText) findViewById( R.id.surname_edit )).getText().toString();
					final String e_phone = ((EditText) findViewById( R.id.telephone_edit )).getText().toString();
					final String date_from_edit = ((EditText) findViewById( R.id.date_from_edit )).getText().toString();
					final String date_to_edit = ((EditText) findViewById( R.id.date_to_edit )).getText().toString();
					final String e_cabins = ((EditText) findViewById( R.id.cabin_edit )).getText().toString();
					final String e_deckchairs = ((EditText) findViewById( R.id.deckchair_edit )).getText().toString();

					final SharedPreferences pref = getPreferences( MODE_PRIVATE );

					if(pref.getString( K_CHECK, "false" ).equals( "false" )){
						// viene ricordato che se ci sono prenotazioni multiple soltanto alla prima verranno assegnate sdraie e cabine
						LayoutInflater factory = LayoutInflater.from( activity );
						final View view = factory.inflate( R.layout.dialog_check_layout,
														   (ViewGroup) activity.findViewById( R.id.dialog_check_layout_root ) );

						new AlertDialog.Builder( activity )
							.setTitle( R.string.attention )
							.setView( view )
							.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
								@Override
								public void onClick( DialogInterface dialog, int whichButton )
								{
									CheckBox check = (CheckBox) view.findViewById( R.id.checkbox_remember );
									if(check.isChecked()){
										Editor editor = pref.edit();
										editor.putString( K_CHECK, "true" );
										editor.commit();
									}

									sendMultiBookings( date_from_edit, date_to_edit, e_name, e_surname, e_phone, e_cabins, e_deckchairs );
								}
							} )
							.create()
							.show();

						((TextView) view.findViewById( R.id.title_text )).setText( R.string.note_multiple_bookings );
					}
					else
						sendMultiBookings( date_from_edit, date_to_edit, e_name, e_surname, e_phone, e_cabins, e_deckchairs );
				}
				else{
					String e_name = ((EditText) findViewById( R.id.name_edit )).getText().toString();
					String e_surname = ((EditText) findViewById( R.id.surname_edit )).getText().toString();
					String e_phone = ((EditText) findViewById( R.id.telephone_edit )).getText().toString();
					String date_from_edit = ((EditText) findViewById( R.id.date_from_edit )).getText().toString();
					String date_to_edit = ((EditText) findViewById( R.id.date_to_edit )).getText().toString();
					String e_cabins = ((EditText) findViewById( R.id.cabin_edit )).getText().toString();
					String e_deckchairs = ((EditText) findViewById( R.id.deckchair_edit )).getText().toString();

					// se i dati sono cambiati invia le modifiche
					if(!name.equals( e_name ) || !surname.equals( e_surname ) || !phone.equals( e_phone ) ||
							!date_from.equals( date_from_edit ) || !date_to.equals( date_to_edit ) ||
							!cabine.equals( e_cabins ) || !sdraie.equals( e_deckchairs )){
						if(e_name.equals( "" )) e_name = "-";
						if(e_surname.equals( "" )) e_surname = "-";
						if(e_phone.equals( "" )) e_phone = "-";

						String old_cabins = ombrelloni.get( 0 ).getBooking( IDPrenotazione ).getCabins() + "";
						String old_deckchairs = ombrelloni.get( 0 ).getBooking( IDPrenotazione ).getDeckchairs() + "";

						Global.net.sendMessage( SocketMsg.MODIFY_BOOKING + "", ombrelloni.get( 0 ).getIDPlace() + "", IDPrenotazione + "",
												date_from_edit, date_to_edit, e_name, e_surname, e_phone, old_cabins,
												e_cabins, old_deckchairs, e_deckchairs );
					}
				}

				break;

			case( R.id.cancelButton ):
				finish();

				break;

			case( R.id.date_from_edit ):
				int day, month, year;
				final EditText from = (EditText) findViewById( R.id.date_from_edit );
				String date = from.getText().toString();
	
				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3, 5 ) ) - 1;
				year = Integer.parseInt( date.substring( 6 ) );
	
				new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((year < now.year) ||
						   (year == now.year && month < now.month) ||
						   (year == now.year && month == now.month && day < now.monthDay)){
							Toast toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// controlla se la data e' cambiata
						String c_date_from = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month + "-" + year;
						if(!c_date_from.equals( from.getText().toString() )){
							from.setText( c_date_from );

							EditText to = (EditText) findViewById( R.id.date_to_edit );
							String c_date_to = to.getText().toString();
							int t_day = Integer.parseInt( c_date_to.substring( 0, 2 ) );
							int t_month = Integer.parseInt( c_date_to.substring( 3, 5 ) );
							int t_year = Integer.parseInt( c_date_to.substring( 6 ) );

							if((year > t_year) ||
							   (year == t_year && month > t_month) ||
							   (year == t_year && month == t_month && day > t_day)){
								to.setText( c_date_from );
								c_date_to = c_date_from;
							}

							updatePlaceStatus( true, 0, false, false, false, 0 );
						}
					}

				}, year, month, day ).show();

				break;

			case( R.id.date_to_edit ):
				final EditText to = (EditText) findViewById( R.id.date_to_edit );
				date = to.getText().toString();

				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3, 5 ) ) - 1;
				year = Integer.parseInt( date.substring( 6 ) );

				new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((year < now.year) ||
						   (year == now.year && month < now.month) ||
						   (year == now.year && month == now.month && day < now.monthDay)){
							Toast toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// controlla se la data e' cambiata
						String c_date_to = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month + "-" + year;
						if(!c_date_to.equals( to.getText().toString() )){
							to.setText( c_date_to );

							EditText from = (EditText) findViewById( R.id.date_from_edit );
							String c_date_from = from.getText().toString();
							int f_day = Integer.parseInt( c_date_from.substring( 0, 2 ) );
							int f_month = Integer.parseInt( c_date_from.substring( 3, 5 ) );
							int f_year = Integer.parseInt( c_date_from.substring( 6 ) );

							if((year < f_year) ||
							   (year == f_year && month < f_month) ||
							   (year == f_year && month == f_month && day < f_day)){
								from.setText( c_date_to );
								c_date_from = c_date_to;
							}

							updatePlaceStatus( true, 0, false, false, false, 0 );
						}
					}

				}, year, month, day ).show();

				break;

			case( R.id.cabin_edit ):
				// apre un number picker creato manualmente in un XML
				LayoutInflater factory = LayoutInflater.from( context );
				View view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				final EditText cabins = (EditText) findViewById( R.id.cabin_edit );
				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( cabins.getText() );

				isCabinOpen = true;

				new AlertDialog.Builder( context )
					.setTitle( R.string.cabin )
					.setView( view )
					.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							String v = value.getText().toString();

							// controlla se e' stata inserita almeno una cifra
							if(value.equals( "" )){
								Toast toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
								toast.setGravity( Gravity.CENTER, 0, 0 );
								toast.show();
							}
							else{
								// controlla se ci sono cambiamenti
								if(!cabins.getText().toString().equals( v )){
									cabins.setText( v );

									((TextView) activity.findViewById( R.id.price_edit )).setText( compute_price() + "" );
								}
							}
						}
					})
					.setNegativeButton( R.string.cancel, null )
					.create()
					.show();

				break;

			case( R.id.deckchair_edit ):
				// apre un number picker creato manualmente in un XML
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				final EditText deckchairs = (EditText) findViewById( R.id.deckchair_edit );
				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( deckchairs.getText() );

				isCabinOpen = false;

				new AlertDialog.Builder( context )
				.setTitle( R.string.deckchair )
				.setView( view )
				.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						String v = value.getText().toString();

						// controlla se e' stata inserita almeno una cifra
						if(value.equals( "" )){
							Toast toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();
						}
						else{
							// controlla se ci sono cambiamenti
							if(!deckchairs.getText().toString().equals( v )){
								deckchairs.setText( v );

								((TextView) activity.findViewById( R.id.price_edit )).setText( compute_price() + "" );
							}
						}
					}
				})
				.setNegativeButton( R.string.cancel, null )
				.create()
				.show();

				break;

			case( R.id.increase ):
				// aumenta il numero di righe o colonne
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				if(value == null)
					value = (EditText) view.findViewById( R.id.value_edit );

				if(isCabinOpen && n_cabins < Global.free_cabins + Integer.parseInt( cabine ))
					value.setText( (++n_cabins) + "" );
				else{
					if(!isCabinOpen && n_deckchairs < Global.free_deckchairs + Integer.parseInt( sdraie ))
						value.setText( (++n_deckchairs) + "" );
				}

				break;

			case( R.id.reduce ):
				// riduce il numero di righe o colonne
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				if(value == null)
					value = (EditText) view.findViewById( R.id.value_edit );

				if(isCabinOpen && n_cabins > 0)
					value.setText( (--n_cabins) + "" );
				else{
					if(!isCabinOpen && n_deckchairs > 0)
						value.setText( (--n_deckchairs) + "" );
				}

				break;
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle state )
	{
		EditText name = (EditText) findViewById( R.id.name_edit );
		state.putString( K_NAME, name.getText().toString() );

		EditText surname = (EditText) findViewById( R.id.surname_edit );
		state.putString( K_SURNAME, surname.getText().toString() );

		EditText telephone = (EditText) findViewById( R.id.telephone_edit );
		state.putString( K_PHONE, telephone.getText().toString() );

		EditText from = (EditText) findViewById( R.id.date_from_edit );
		state.putString( K_DATE_FROM, from.getText().toString() );

		EditText to = (EditText) findViewById( R.id.date_to_edit );
		state.putString( K_DATE_TO, to.getText().toString() );

		TextView status = (TextView) findViewById( R.id.status_value_text );
		state.putString( K_STATE, status.getText().toString() );

		TextView cabins = (TextView) findViewById( R.id.cabin_edit );
		state.putString( K_CABINS, cabins.getText().toString() );

		TextView deckchairs = (TextView) findViewById( R.id.deckchair_edit );
		state.putString( K_DECKCHAIRS, deckchairs.getText().toString() );

		TextView price = (TextView) findViewById( R.id.price_edit );
		state.putString( K_PRICE, price.getText().toString() );

		super.onSaveInstanceState( state );
	}

	/** calcola il prezzo della prenotazione
	 * 
	 * @return il prezzo
	*/
	private static synchronized float compute_price()
	{
		return Global.bm.compute_price( date_from, date_to, n_cabins, n_deckchairs );
	}

	/** aggiorna lo stato della prenotazione
	 * 
	 * @param inside - TRUE se richiamata da questa activity, FALSE altrimenti
	 * @param IDPlace - ID del posto a cui e' associata la prenotazione
	 * @param deletePlace - TRUE se il posto e' stato cancellato, FALSE altrimenti
	 * @param deleteAllPlaces - TRUE se tutti i posti sono stati cancellati, FALSE altrimenti
	 * @param deleteBooking - TRUE se la prenotazione e' stata cancellata, FALSE altrimenti
	 * @param ID - ID della prenotazione cancellata
	*/
	public static synchronized void updatePlaceStatus( boolean inside, int IDPlace, final boolean deletePlace,
													   final boolean deleteAllPlaces, boolean deleteBooking, int ID )
	{
		if(ombrelloni == null || ombrelloni.size() == 0)
			return;

		final int index = checkIDPlace( IDPlace );
		if(inside || deleteAllPlaces || (isOpen && index >= 0)){
			if(deleteAllPlaces || deletePlace){
				LayoutInflater factory = LayoutInflater.from( activity );
				View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

				if(deleteAllPlaces)
					((TextView) v.findViewById( R.id.title_text )).setText( R.string.deleted_out_places );
				else{
					String msg = activity.getString( R.string.deleted_out_place ).replace( "X", ombrelloni.get( index ).getPosition() );
					((TextView) v.findViewById( R.id.title_text )).setText( msg );
				}

				new AlertDialog.Builder( activity )
					.setTitle( R.string.attention )
					.setView( v )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int which )
						{
							if(deletePlace)
								ombrelloni.remove( index );

							// se dopo rimosso l'ombrellone non ce ne sono piu' chiude tutto
							if(deleteAllPlaces || ombrelloni.size() == 0){
								dialog.dismiss();
								activity.finish();
							}
						}
					} )
					.create()
					.show();
			}
			else{
				if(deleteBooking && ID == IDPrenotazione){
					if(notify == null){
						LayoutInflater factory = LayoutInflater.from( activity );
						View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

						notify = new AlertDialog.Builder( activity )
							.setTitle( R.string.attention )
							.setView( v )
							.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
								@Override
								public void onClick( DialogInterface dialog, int which )
								{
									dialog.dismiss();
									activity.finish();
								}
							} )
							.create();

						((TextView) v.findViewById( R.id.title_text )).setText( R.string.deleted_out_booking );
					}

					notify.show();
				}

				int stato = Ombrellone.FREE;

				if(inside){
					if(current_date_from != null && current_date_to != null){
						int c_f_day = Integer.parseInt( current_date_from.substring( 0, 2 ) );
						int c_f_month = Integer.parseInt( current_date_from.substring( 3, 5 ) );
						int c_f_year = Integer.parseInt( current_date_from.substring( 6 ) );

						int c_t_day = Integer.parseInt( current_date_to.substring( 0, 2 ) );
						int c_t_month = Integer.parseInt( current_date_to.substring( 3, 5 ) );
						int c_t_year = Integer.parseInt( current_date_to.substring( 6 ) );

						for(int i = ombrelloni.size() - 1; i >= 0; i--)
							ombrelloni.get( i ).updateStatus( c_f_day, c_f_month, c_f_year, c_t_day, c_t_month, c_t_year );
					}

					for(int i = ombrelloni.size() - 1; i >= 0; i--){
						if(ombrelloni.get( i ).checkStatus( IDPrenotazione,
															((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString(),
															((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString() )
															== Ombrellone.OCCUPIED){
							stato = Ombrellone.OCCUPIED;
							break;
						}
					}
				}
				else{
					for(int i = ombrelloni.size() - 1; i >= 0; i--){
						if(ombrelloni.get( i ).getStatus() == Ombrellone.OCCUPIED){
							stato = Ombrellone.OCCUPIED;
							break;
						}
					}
				}

				TextView price = (TextView) activity.findViewById( R.id.price_edit );

				if(stato == Ombrellone.FREE){
					((Button) activity.findViewById( R.id.okButton )).setEnabled( true );
					TextView status = (TextView) activity.findViewById( R.id.status_value_text );
					status.setTextColor( Color.GREEN );
					status.setText( R.string.free );
				}
				else{
					TextView status = (TextView) activity.findViewById( R.id.status_value_text );
					status.setTextColor( Color.RED );
					status.setText( R.string.occupied );

					String c_date_from = ((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString();
					String c_date_to = ((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString();
					if(type == CREATE || (type == MODIFY && (!c_date_from.equals( date_from ) || !c_date_to.equals( date_to ))))
						((Button) activity.findViewById( R.id.okButton )).setEnabled( false );
					else
						((Button) activity.findViewById( R.id.okButton )).setEnabled( true );
				}

				// inserisce/modifica i parametri della prenotazione
				if(type == MODIFY){
					Booking u = ombrelloni.get( 0 ).getBooking( IDPrenotazione );

					date_from = u.getDateFrom();
					date_to = u.getDateTo();

					if((name = u.getName()).equals( "-" ))
						name = "";

					EditText e_name = (EditText) activity.findViewById( R.id.name_edit );
					e_name.setText( name );
					e_name.setSelection( e_name.getText().length() );

					if((surname = u.getSurname()).equals( "-" ))
						surname = "";

					EditText e_surname = (EditText) activity.findViewById( R.id.surname_edit );
					e_surname.setText( surname );

					if((phone = u.getPhone()).equals( "-" ))
						phone = "";

					n_cabins = u.getCabins();
					n_deckchairs = u.getDeckchairs();

					cabine = n_cabins + "";
					sdraie = n_deckchairs + "";

					EditText telephone = (EditText) activity.findViewById( R.id.telephone_edit );
					telephone.setText( phone );

					((EditText) activity.findViewById( R.id.cabin_edit )).setText( u.getCabins() + "" );
					((EditText) activity.findViewById( R.id.deckchair_edit )).setText( u.getDeckchairs() + "" );

					price.setText( u.getPrice() + "" );
				}
				else
					price.setText( compute_price() + "" );
			}
		}
	}

	/** controlla se l'ID del posto da aggiornare e' uno di quelli selezionati
	 * 
	 * @param IDPlace - ID del posto da aggiornare
	 * 
	 * @return l'indice del posto trovato, -1 altrimenti
	*/
	private static int checkIDPlace( int IDPlace )
	{
		for(int i = ombrelloni.size() - 1; i >= 0; i--)
			if(ombrelloni.get( i ).getIDPlace() == IDPlace)
				return i;

		return -1;
	}

	/** modifica lo stato dell'activity
	 * 
	 * @param open - TRUE se l'activity e' attiva, FALSE altrimenti
	*/
	private synchronized void setOpen( boolean open )
	{
		isOpen = open;
	}

	/** restituisce lo stato dell'activity
	 * 
	 * @return TRUE se l'activity e' attiva, FALSE altrimenti
	*/
	public static synchronized boolean isOpen()
	{
		return isOpen;
	}

	/** aggiunge un nuovo messaggio nella coda
	 * 
	 * @param message - il messaggio da aggiungere
	*/
	private static synchronized void addMessage( String message )
	{
		texts.add( message );
	}

	/** rimuove un messaggio dalla coda
	 * 
	 * @return il prossimo messaggio da visualizzare
	*/
	private static synchronized String getMessage()
	{
		if(texts.size() > 0)
			return texts.remove( 0 );
		else
			return null;
	}

	/** aggiunge i posti da gestire
	 * 
	 * @param places - lista di posti
	 * @param place - il posto
	*/
	public static void addPlaces( ArrayList< Ombrellone> places, Ombrellone place )
	{
		if(ombrelloni == null)
			ombrelloni = new ArrayList<Ombrellone>();
		else
			ombrelloni.clear();

		if(places == null)
			ombrelloni.add( place );
		else
			ombrelloni = places;
	}

	/** apre/chiude il dialog
	 * 
	 * @param toOpen - TRUE se deve essere aperto, FALSE altrimenti
	 * @param message - il messaggio da visualizzare se viene aperto, null se viene chiuso
	*/
	private static synchronized void setDialogOpen( boolean toOpen, String message )
	{
		if(toOpen){
			if(dialog.isShowing())
				addMessage( message );
			else{
				text.setText( message );
				dialog.show();
			}
		}
		else{
			String msg = getMessage();
			if(msg == null)
				dialog.dismiss();
			else
				text.setText( msg );

			updateNext();
		}
	}

	/** aggiorna il prossimo ombrellone da visualizzare;
		se arrivati in fondo chiude l'activity */
	private static synchronized void updateNext()
	{
		if(++next == ombrelloni.size()){
			if(dialog != null)
				dialog.dismiss();

			activity.finish();
		}
	}

	/** apre un dialog contenente un messaggio
	 * 
	 * @param message - messaggio da visualizzare
	*/
	private static void openDialog( String message )
	{
		if(dialog == null){
			LayoutInflater factory = LayoutInflater.from( activity );
			View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

			dialog = new AlertDialog.Builder( activity )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( "Ok", null )
				.create();

			dialog.setOnShowListener( new DialogInterface.OnShowListener(){
				@Override
				public void onShow( DialogInterface d )
				{
					dialog.getButton( AlertDialog.BUTTON_POSITIVE ).setOnClickListener( new OnClickListener(){
						@Override
						public void onClick( View view )
						{
							setDialogOpen( false, null );
			            }
			        });
			    }
			} );

			text = (TextView) v.findViewById( R.id.title_text );
		}

		setDialogOpen( true, message );
	}

	/** inserisce il responso della prenotazione
	 *
	 * @param accepted - TRUE se la prenotazione e' stata accettata, FALSE altrimenti
	 * @param data - vettore contenente i dati della prenotazione
	*/
	public static void check_response( boolean accepted, String data[] )
	{
		if(accepted){
			String name = ((EditText) activity.findViewById( R.id.name_edit )).getText().toString();
			String surname = ((EditText) activity.findViewById( R.id.surname_edit )).getText().toString();
			String phone = ((EditText) activity.findViewById( R.id.telephone_edit )).getText().toString();
			String date_from = ((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString();
			String date_to = ((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString();

			// inserisce o modifica la prenotazione
			if(type == CREATE){
				if(next_place == 0){
					int cabins = Integer.parseInt( ((EditText) activity.findViewById( R.id.cabin_edit )).getText().toString() );
					int deckchairs = Integer.parseInt( ((EditText) activity.findViewById( R.id.deckchair_edit )).getText().toString() );

					Global.bm.addBooking( ombrelloni.get( 0 ).getIDPlace(), Integer.parseInt( data[0] ),
										  date_from, date_to, name, surname, phone,
										  cabins, deckchairs, current_date_from, current_date_to, data );
				}
				else
					Global.bm.addBooking( ombrelloni.get( next_place ).getIDPlace(), Integer.parseInt( data[0] ),
										  date_from, date_to, name, surname, phone,
										  0, 0, current_date_from, current_date_to, data );

				if(ombrelloni.size() > 1){
					openDialog( activity.getString( R.string.booking_multi1_accepted ) + " " +
												    ombrelloni.get( next_place ).getPosition() + " " +
												    activity.getString( R.string.booking_multi2_accepted ) );
				}
				else{
					showToast( activity.getString( R.string.booking_accepted ), Gravity.CENTER );
					updateNext();
				}
			}
			else{
				int cabins = Integer.parseInt( ((EditText) activity.findViewById( R.id.cabin_edit )).getText().toString() );
				int deckchairs = Integer.parseInt( ((EditText) activity.findViewById( R.id.deckchair_edit )).getText().toString() );

				Global.bm.modifyBooking( ombrelloni.get( 0 ).getIDPlace(), IDPrenotazione, date_from, date_to,
										 name, surname, phone, cabins, deckchairs, current_date_from, current_date_to, data );

				showToast( activity.getString( R.string.booking_modified_accepted ), Gravity.CENTER );

				updateNext();
			}
		}
		else
			openDialog( ErrorMessage.getError( data[0].charAt( 0 ), activity ).replace( "X", ombrelloni.get( next_place ).getPosition() ) );

		next_place++;
	}

	/** mostra il toast dell'aggiornamento
	 * 
	 * @param text - il testo
	 * @param gravity - la posizione del toast
	*/
	private static void showToast( String text, int gravity )
	{
		if(toast == null)
			toast = Toast.makeText( activity, "", Toast.LENGTH_LONG );

		toast.setText( text );
		toast.setGravity( gravity, 0, 0 );
		toast.show();
	}

	/** forza la chiusura dell'activity in seguito a un errore */
	public synchronized static void close()
	{
		if(isOpen)
			activity.finish();
	}
}