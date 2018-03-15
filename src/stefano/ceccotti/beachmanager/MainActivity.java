
package stefano.ceccotti.beachmanager;

import java.util.Date;
import java.util.List;

import stefano.ceccotti.beachmanager.engine.BeachManager;
import stefano.ceccotti.beachmanager.engine.DataReceiver;
import stefano.ceccotti.beachmanager.engine.NetworkManager;
import stefano.ceccotti.beachmanager.entities.Booking;
import stefano.ceccotti.beachmanager.entities.BookingAdapter;
import stefano.ceccotti.beachmanager.entities.LogAdapter;
import stefano.ceccotti.beachmanager.entities.Ombrellone;
import stefano.ceccotti.beachmanager.entities.Rate;
import stefano.ceccotti.beachmanager.entities.RateAdapter;
import stefano.ceccotti.beachmanager.utils.ErrorMessage;
import stefano.ceccotti.beachmanager.utils.Global;
import stefano.ceccotti.beachmanager.utils.Log;
import stefano.ceccotti.beachmanager.utils.SocketMsg;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnClickListener, OnMenuItemClickListener, OnCheckedChangeListener, android.support.v7.widget.PopupMenu.OnMenuItemClickListener
{
	/* un generico progressDialog */
	private static ProgressDialog dialog;
	/* il toast per le segnalazioni */
	private static Toast toast;
	/* dialog per il login */
	private static Dialog login;
	/* bottone per la creazione di nuove tariffe o prenotazioni */
	private static Button button;
	/* dialog per la lista delle prenotazioni, delle tariffe e dei log */
	private static AlertDialog booking_list, rate_list, log_list;
	/* adapter per la gestione delle prenotazioni */
	public static BookingAdapter booking_adapter;
	/* adapter per la gestione delle tariffe */
	public static RateAdapter rate_adapter;
	/* adapter per la gestione dei file di log */
	public static LogAdapter log_adapter;
	/* lista contenente gli elementi da visualizzare */
	private static ListView listView;
	/* ID della prenotazione, della tariffa o del log selezionato */
	private int IDPrenotazione, IDRate, IDLog;
	/* il menu' dell'activity */
	private static Menu m_menu;
	/* l'ombrellone selezionato */
	private static Ombrellone m_ombrellone;
	/* contiene il valore del numero di righe e colonne (modalita' edit) */
	private EditText value;
	/* determina se sono state selezionate le tariffe o i log */
	private static boolean isRateOpen = false, isLogOpen = false;
	/* questa activity */
	private static Activity activity;
	/* area per inserimento cabine e sdraie */
	private static EditText cabins, deckchairs;
	/* determina se e' stata selezionata la modifica delle righe */
	private boolean isRowsEdit = false;
	/* la view corrente */
	private View current_view;
	/* numero di notifiche visibili */
	private static int count = 0;

	/* il contesto dell'activity */
	private final Context context = this;
	/* tipi di dialog */
	private static final int DIALOG_LOGIN = 1, NETWORK_OFF = 2, NETWORK_DISCONNECTED = 3;
	/* chiave per associare le preferenze e i salvataggi */
	private static final String K_ADDRESS = "MAIN_ADDRESS", K_LOGIN = "MAIN_LOGIN", K_PASSWORD = "MAIN_PASSWORD",
								K_DATE_FROM = "MAIN_DATE_FROM", K_DATE_TO = "MAIN_DATE_TO", K_COLUMNS = "MAIN_COLUMNS",
								K_ROWS = "MAIN_ROWS";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		//requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView( R.layout.main_activity );

		Global.isInBackground = false;

		// ottiene la versione della app (da usare come valore da inviare al server??)
		/*PackageInfo pInfo = null;
		try{
			pInfo = getPackageManager().getPackageInfo( getPackageName(), 0 );
			String version = pInfo.versionName;
			NetworkManager.VERSION = Double.parseDouble( pInfo.versionName );
			Log.d( "MAIN", "VERSION: " + version );
		}catch( NameNotFoundException e ){
			e.printStackTrace();
		}*/

		// devo aggiungere un broadcast receiver per farlo avviare all'avvio del telefono (per adesso non serve)

		dialog = new ProgressDialog( context );
		dialog.setIndeterminate( true );

		/* custom toast
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate( R.layout.toast_layout, (ViewGroup) findViewById( R.id.toast_layout_root ) );

		// il text view va caricato per dargli un testo, altrimenti va in crash
		TextView text = (TextView) layout.findViewById( R.id.text );
		text.setText( "This is a custom toast" );

		toast = new Toast( context );
		toast.setView( layout );
		toast.setDuration( Toast.LENGTH_LONG );*/

		Global.init( this, false );

		// modifica l'altezza della barra in alto
		LinearLayout layout = (LinearLayout) findViewById( R.id.linear_bar );
		((LinearLayout.LayoutParams) layout.getLayoutParams()).height = (int)(Global.sizehBox * 2.5f);

		Time now = new Time();
		now.setToNow();
		now.month++;

		EditText from = (EditText) findViewById( R.id.from_edit );
		EditText to = (EditText) findViewById( R.id.to_edit );

		from.setText( ((now.monthDay < 10) ? "0" : "") + now.monthDay + "-" + ((now.month < 10) ? "0" : "") + now.month + "-" + now.year );
		from.setOnClickListener( this );
		//from.setTextSize( 19 * (Global.ratioW / Global.ratioH) );
		from.setTextSize( 19 );
		from.setTextScaleX( Global.ratioW );

		to.setText( ((now.monthDay < 10) ? "0" : "") + now.monthDay + "-" + ((now.month < 10) ? "0" : "") + now.month + "-" + now.year );
		to.setOnClickListener( this );
		to.setTextSize( 19 );
		to.setTextScaleX( Global.ratioW );

		login = onCreateDialog( DIALOG_LOGIN );

		// TODO utilizzare ImageButton in caso di bottone con immagine
		Button button = (Button) findViewById( R.id.connectionButton );
		button.setOnClickListener( this );

		if(savedInstanceState != null){
			// ripristina lo stato delle view testuali
			LayoutInflater factory = LayoutInflater.from( this );
			View v = factory.inflate( R.layout.dialog_login_layout, (ViewGroup) findViewById( R.id.dialog_login_layout_root ) );

			EditText address = (EditText) v.findViewById( R.id.network_address_edit );
			address.setText( savedInstanceState.getString( K_ADDRESS ) );

			EditText username = (EditText) v.findViewById( R.id.username_edit );
			username.setText( savedInstanceState.getString( K_LOGIN ) );

			EditText password = (EditText) v.findViewById( R.id.password_edit );
			password.setText( savedInstanceState.getString( K_PASSWORD ) );

			from.setText( savedInstanceState.getString( K_DATE_FROM ) );
			to.setText( savedInstanceState.getString( K_DATE_TO ) );

			EditText rows = (EditText) findViewById( R.id.rows_edit );
			rows.setText( savedInstanceState.getString( K_ROWS ) );

			EditText columns = (EditText) findViewById( R.id.columns_edit );
			columns.setText( savedInstanceState.getString( K_COLUMNS ) );
		}

		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable( new ColorDrawable( Color.parseColor( "#0099FF" ) ) );
		bar.setTitle( Html.fromHtml( "<font color=\"white\">" + getString( R.string.app_name ) + "</font>" ) );

		activity = MainActivity.this;

		MyHandler handler = new MyHandler();

		if(Global.bm == null){
			Global.bm = new BeachManager( activity );

			// creazione thread responsabile della ricezione dei dati dal server
			Thread t = new Thread( new DataReceiver( context, handler ) );
			t.setPriority( Thread.MAX_PRIORITY );
			t.start();

			//startService( new Intent( this, DataService.class ) );
		}
		else{
			DataReceiver.setHandler( handler );

			Global.bm.re_build( this );
			Global.bm.updateView( from.getText().toString(), to.getText().toString() );

			if(Global.connected){
				button.setText( R.string.disconnection );

				if(!Global.edit_mode){
					from.setVisibility( EditText.VISIBLE );
					to.setVisibility( EditText.VISIBLE );
				}
				else{
					Global.bm.setEditMode( this, true, false );

					EditText rows = (EditText) findViewById( R.id.rows_edit );
					rows.setVisibility( EditText.VISIBLE );
					rows.setOnClickListener( this );
					rows.setTextSize( 19 * (Global.ratioW / Global.ratioH) );

					EditText columns = (EditText) findViewById( R.id.columns_edit );
					columns.setVisibility( EditText.VISIBLE );
					columns.setOnClickListener( this );
					columns.setTextSize( 19 * (Global.ratioW / Global.ratioH) );

					from.setVisibility( EditText.GONE );
					to.setVisibility( EditText.GONE );
				}
			}
		}

		getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
	}

	@Override
	protected void onSaveInstanceState( Bundle state )
	{
		LayoutInflater factory = LayoutInflater.from( this );
		View v = factory.inflate( R.layout.dialog_login_layout, (ViewGroup) findViewById( R.id.dialog_login_layout_root ) );

		EditText address = (EditText) v.findViewById( R.id.network_address_edit );
		state.putString( K_ADDRESS, address.getText().toString() );

		EditText username = (EditText) v.findViewById( R.id.username_edit );
		state.putString( K_LOGIN, username.getText().toString() );

		EditText password = (EditText) v.findViewById( R.id.password_edit );
		state.putString( K_PASSWORD, password.getText().toString() );

		EditText from = (EditText) findViewById( R.id.from_edit );
		state.putString( K_DATE_FROM, from.getText().toString() );

		EditText to = (EditText) findViewById( R.id.to_edit );
		state.putString( K_DATE_TO, to.getText().toString() );

		EditText rows = (EditText) findViewById( R.id.rows_edit );
		state.putString( K_ROWS, rows.getText().toString() );

		EditText columns = (EditText) findViewById( R.id.columns_edit );
		state.putString( K_COLUMNS, columns.getText().toString() );

		super.onSaveInstanceState( state );
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged( newConfig );

		Global.init( null, true );

		Global.bm.updatePadding();

		// modifica l'altezza della barra in alto
		LinearLayout layout = (LinearLayout) findViewById( R.id.linear_bar );
		((LinearLayout.LayoutParams) layout.getLayoutParams()).height = (int)(Global.sizehBox * 2.5f);

		EditText from = (EditText) findViewById( R.id.from_edit );
		EditText to = (EditText) findViewById( R.id.to_edit );

		//from.setTextSize( 19 * (Global.ratioW / Global.ratioH) );
		from.setTextSize( 19 );
		from.setTextScaleX( Global.ratioW );

		to.setTextSize( 19 );
		to.setTextScaleX( Global.ratioW );
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

		NotificationManager manager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
		manager.cancel( SocketMsg.ADD_PLACE );
		manager.cancel( SocketMsg.MODIFY_PLACE );
		manager.cancel( SocketMsg.DELETE_PLACE );
		manager.cancel( SocketMsg.DELETE_ALL_PLACES );
		manager.cancel( SocketMsg.ADD_BOOKING );
		manager.cancel( SocketMsg.MODIFY_BOOKING );
		manager.cancel( SocketMsg.DELETE_BOOKING );
		manager.cancel( SocketMsg.ADD_TARIFF );
		manager.cancel( SocketMsg.MODIFY_TARIFF );
		manager.cancel( SocketMsg.DELETE_TARIFF );
		manager.cancel( SocketMsg.MODIFY_DATA );

		setBadge( context, count = 0 );

		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		if(m_menu != null){
			m_menu.clear();
			m_menu = null;
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		if(m_menu == null){
			m_menu = menu;
			getMenuInflater().inflate( R.menu.activity_main, menu );
		}

		if(Global.edit_mode){
			menu.getItem( 0 ).setVisible( false );
			menu.getItem( 1 ).setVisible( false);
			menu.getItem( 2 ).setVisible( true );
			menu.getItem( 3 ).setVisible( true );
		}
		else{
			if(!Global.connected){
				menu.getItem( 0 ).setVisible( false );
				menu.getItem( 1 ).setVisible( false );
				menu.getItem( 2 ).setVisible( false );
				menu.getItem( 3 ).setVisible( false );
				menu.getItem( 4 ).setVisible( false );
				menu.getItem( 6 ).setVisible( false );
			}
			else{
				menu.getItem( 0 ).setVisible( true );
				menu.getItem( 1 ).setVisible( true );
				menu.getItem( 2 ).setVisible( false );
				menu.getItem( 3 ).setVisible( false );
				menu.getItem( 4 ).setVisible( true );
				menu.getItem( 6 ).setVisible( true );
			}
		}

		return true;
	}

	/** assegna all'icona della app il numero di notifiche non ancora visualizzate (per rimuoverle basta invocarlo con count = 0)
	 * 
	 * @param context	il contesto
	 * @param count		numero di notifiche
	*/
	public static void setBadge( Context context, int count )
	{
		String launcherClassName = null;
		PackageManager pm = context.getPackageManager();

		Intent intent = new Intent( Intent.ACTION_MAIN );
		intent.addCategory( Intent.CATEGORY_LAUNCHER );

		List<ResolveInfo> resolveInfos = pm.queryIntentActivities( intent, 0 );
		for(ResolveInfo resolveInfo : resolveInfos){
			String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
			if(pkgName.equalsIgnoreCase( context.getPackageName() ))
				launcherClassName = resolveInfo.activityInfo.name;
		}

		if(launcherClassName != null){
			intent = new Intent( "android.intent.action.BADGE_COUNT_UPDATE" );
			intent.putExtra( "badge_count", count );
			intent.putExtra( "badge_count_package_name", context.getPackageName() );
			intent.putExtra( "badge_count_class_name", launcherClassName );
			context.sendBroadcast( intent );
		}
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() ){
			case( R.id.menu_rates ):
				// apre la lista delle tariffe
				LayoutInflater factory = LayoutInflater.from( this );
				View v = factory.inflate( R.layout.dialog_list_layout, (ViewGroup) findViewById( R.id.dialog_list_layout_root ) );

				if(rate_list == null){
					rate_adapter = new RateAdapter( this, Global.bm.getRatesList() );

					//((LinearLayout.LayoutParams) listView.getLayoutParams()).height = (int)(Global.sizehBox * 100);

					rate_list = new AlertDialog.Builder( this )
					.setTitle( R.string.list_tariffs )
					.setView( v )
					.setPositiveButton( R.string.close, new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							isRateOpen = false;
						}
					} )
					.create();
				}

				listView = (ListView) v.findViewById( R.id.listView );
				listView.setAdapter( rate_adapter );

				registerForContextMenu( listView );

				isRateOpen = true;

				Button b = (Button) v.findViewById( R.id.new_button );
				b.setText( R.string.add_tariff );
				b.setOnClickListener( this );

				((CheckBox) v.findViewById( R.id.checkbox_filter )).setVisibility( CheckBox.GONE );

				rate_list.show();
				rate_list.getWindow().setLayout( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT );

				break;

			case( R.id.menu_settings ):
				// apre il menu' delle impostazioni
				Intent i = new Intent( this, PrefActivity.class );
				startActivity( i );

				break;

			case( R.id.menu_edit ):
				// attiva la modalita' edit
				Global.edit_mode = true;
				Global.bm.setEditMode( this, true, false );

				// modifica il menu' per gestire la modalita' edit
				onCreateOptionsMenu( m_menu );

				((EditText) findViewById( R.id.from_edit )).setVisibility( EditText.GONE );
				((EditText) findViewById( R.id.to_edit )).setVisibility( EditText.GONE );

				EditText rows = (EditText) findViewById( R.id.rows_edit );
				rows.setVisibility( EditText.VISIBLE );
				rows.setTextSize( 19 * (Global.ratioW / Global.ratioH) );
				rows.setOnClickListener( this );

				EditText columns = (EditText) findViewById( R.id.columns_edit );
				columns.setVisibility( EditText.VISIBLE );
				columns.setOnClickListener( this );
				columns.setTextSize( 19 * (Global.ratioW / Global.ratioH) );

				break;

			case( R.id.menu_save ):
				// apre un dialog per essere sicuri di salvare le modifiche
				factory = LayoutInflater.from( activity );
				v = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

				if(Global.bm.isManually())
					((TextView) v.findViewById( R.id.title_text )).setText( getString( R.string.save_changes_manually ) + 
																			getString( R.string.menu_settings ) + " -> " + 
																			getString( R.string.numeration_title ) );
				else
					((TextView) v.findViewById( R.id.title_text )).setText( R.string.save_changes );

				new AlertDialog.Builder( activity )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface d, int whichButton )
					{
						d.dismiss();

						dialog.setMessage( getString( R.string.synchronization ) + "..." );
						dialog.show();

						Global.bm.setEditMode( activity, false, true );

						((EditText) findViewById( R.id.from_edit )).setVisibility( EditText.VISIBLE );
						((EditText) findViewById( R.id.to_edit )).setVisibility( EditText.VISIBLE );

						((EditText) findViewById( R.id.rows_edit )).setVisibility( EditText.GONE );
						((EditText) findViewById( R.id.columns_edit )).setVisibility( EditText.GONE );
					}
				})
				.setNegativeButton( R.string.cancel, null )
				.create()
				.show();

				break;

			case( R.id.menu_cancel ):
				factory = LayoutInflater.from( context );
				v = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

				((TextView) v.findViewById( R.id.title_text )).setText( R.string.cancel_edit_mode );

				new AlertDialog.Builder( context )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						Global.edit_mode = false;
						Global.bm.setEditMode( activity, false, false );

						((EditText) findViewById( R.id.from_edit )).setVisibility( EditText.VISIBLE );
						((EditText) findViewById( R.id.to_edit )).setVisibility( EditText.VISIBLE );

						((EditText) findViewById( R.id.rows_edit )).setVisibility( EditText.GONE );
						((EditText) findViewById( R.id.columns_edit )).setVisibility( EditText.GONE );

						onCreateOptionsMenu( m_menu );
					}
				})
				.setNegativeButton( "No", null )
				.create()
				.show();

				break;

			case( R.id.menu_overflow ):
				// apre il menu' aggiuntivo
				PopupMenu popup = new PopupMenu( context, findViewById( R.id.menu_overflow ) );
			    MenuInflater inflater = popup.getMenuInflater();
			    if(!Global.connected)
			    	inflater.inflate( R.menu.activity_main_start, popup.getMenu() );
			    else
			    	inflater.inflate( R.menu.activity_main_connected, popup.getMenu() );

			    popup.setOnMenuItemClickListener( this );
			    popup.show();

				break;
		}

		return super.onOptionsItemSelected( item );
	}

	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event )
	{
		switch( keyCode ){
			case( KeyEvent.KEYCODE_MENU ):
				if(Global.connected){
					// apre il menu' aggiuntivo
					PopupMenu popup = new PopupMenu( context, findViewById( R.id.menu_overflow ) );
				    MenuInflater inflater = popup.getMenuInflater();
				    inflater.inflate( R.menu.activity_main_connected, popup.getMenu() );

				    popup.setOnMenuItemClickListener( this );
				    popup.show();

				    return true;
				}

				break;

			case( KeyEvent.KEYCODE_BACK ):
				LayoutInflater factory = LayoutInflater.from( this );
				View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

				((TextView) v.findViewById( R.id.title_text )).setText( R.string.close_app_dialog );

				// inserisce una dialog per essere sicuri di chiudere l'applicazione
				new AlertDialog.Builder( context )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						activity.finish();
					}
				})
				.setNegativeButton( "No", null )
				.create()
				.show();

				return true;
		}

		return super.onKeyUp( keyCode, event ); 
	}

	@Override
	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
	{
		super.onCreateContextMenu( menu, v, menuInfo );

		if(Global.edit_mode){
			m_ombrellone = Global.bm.getPlaceByPosition( (String) v.getTag() );
			if(m_ombrellone == null)
				return;

			m_ombrellone.setPressed( true );

			switch( Global.bm.getPlacesEditStatus( m_ombrellone.getEditStatus(), m_ombrellone.getStatus() ) ){
				case( BeachManager.ADD_SINGLE ):
					menu.setHeaderTitle( getString( R.string.options ) + " " + m_ombrellone.getPosition() );
					menu.add( Menu.NONE, 4, Menu.NONE, R.string.add_place );
					menu.add( Menu.NONE, 2, Menu.NONE, R.string.view_status );
					break;

				case( BeachManager.DELETE_SINGLE ):
					menu.setHeaderTitle( getString( R.string.options ) + " " + m_ombrellone.getPosition() );
					menu.add( Menu.NONE, 5, Menu.NONE, R.string.delete_place );
					menu.add( Menu.NONE, 2, Menu.NONE, R.string.view_status );
					break;

				case( BeachManager.ADD_MULTI ):
					menu.setHeaderTitle( R.string.options_multi );
					menu.add( Menu.NONE, 6, Menu.NONE, R.string.add_places );
					break;

				case( BeachManager.DELETE_MULTI ):
					menu.setHeaderTitle( R.string.options_multi );
					menu.add( Menu.NONE, 7, Menu.NONE, R.string.delete_places );
					menu.add( Menu.NONE, 3, Menu.NONE, R.string.modify_place_price );
					break;

				case( BeachManager.NULL ):
					// inserisce una dialog per indicare di aver sbagliato la selezione
					LayoutInflater factory = LayoutInflater.from( context );
					View view = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

					((TextView) view.findViewById( R.id.title_text )).setText( R.string.error_edit_selection );

					new AlertDialog.Builder( context )
					.setTitle( R.string.attention )
					.setView( view )
					.setPositiveButton( "Ok", null /*new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							Global.bm.setPressed( false );
						}
					}*/ )
					.create()
					.show();

					break;
			}
		}
		else{
			if(v.getTag() != null){
				m_ombrellone = Global.bm.getPlaceByID( v.getId() );
				if(m_ombrellone == null)
					return;

				m_ombrellone.setPressed( true );

				switch( Global.bm.getPlacesStatus( m_ombrellone.getStatus() ) ){
					case( BeachManager.SINGLE ):
						menu.setHeaderTitle( getString( R.string.selected ) + " " + m_ombrellone.getPosition() );
						if(m_ombrellone.getStatus() == Ombrellone.FREE)
							menu.add( Menu.NONE, 0, Menu.NONE, R.string.add_booking );
						menu.add( Menu.NONE, 1, Menu.NONE, R.string.view_bookings );
						menu.add( Menu.NONE, 2, Menu.NONE, R.string.view_status );

						break;

					case( BeachManager.MULTI ):
						menu.setHeaderTitle( getString( R.string.options_multi ) );
						menu.add( Menu.NONE, 0, Menu.NONE, R.string.add_booking );
						menu.add( Menu.NONE, 3, Menu.NONE, R.string.modify_place_price );

						break;

					case( BeachManager.NULL ):
						menu.setHeaderTitle( getString( R.string.options_multi ) );
						menu.add( Menu.NONE, 3, Menu.NONE, R.string.modify_place_price );

						break;
				}
			}
			else{
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

				if(isRateOpen){
					IDRate = rate_adapter.getItem( info.position ).getIDRate();

					menu.setHeaderTitle( getString( R.string.tariff ) + ": " + (info.position + 1) );

					menu.add( Menu.NONE, 5, Menu.NONE, R.string.modify_tariff );
					menu.getItem( 0 ).setOnMenuItemClickListener( this );

					menu.add( Menu.NONE, 6, Menu.NONE, R.string.delete_tariff );
					menu.getItem( 1 ).setOnMenuItemClickListener( this );
				}
				else{
					if(isLogOpen){
						IDLog = Global.getLogID( info.position );
						menu.setHeaderTitle( getString( R.string.log ) + ": " + (info.position + 1) );

						menu.add( Menu.NONE, 7, Menu.NONE, R.string.delete_log );
						menu.getItem( 0 ).setOnMenuItemClickListener( this );
					}
					else{
						IDPrenotazione = booking_adapter.getItem( info.position ).getIDPrenotazione();

						menu.setHeaderTitle( getString( R.string.booking ) + ": " + (info.position + 1) );

						menu.add( Menu.NONE, 3, Menu.NONE, R.string.modify_booking );
						menu.getItem( 0 ).setOnMenuItemClickListener( this );

						menu.add( Menu.NONE, 4, Menu.NONE, R.string.delete_booking );
						menu.getItem( 1 ).setOnMenuItemClickListener( this );
					}
				}
			}
		}
    }

	@Override
	public boolean onContextItemSelected( MenuItem item )
	{
		switch( item.getItemId() ){
			case( 0 ):
				// nuova prenotazione
				BookingActivity.addPlaces( Global.bm.getSelectedPlaces(), null );
				BookingActivity.type = BookingActivity.CREATE;
				BookingActivity.date_from = ((EditText) findViewById( R.id.from_edit )).getText().toString();
				BookingActivity.date_to = ((EditText) findViewById( R.id.to_edit )).getText().toString();

				Intent i = new Intent( this, BookingActivity.class );
				startActivity( i );

				break;

			case( 1 ):
				// visualizza le prenotazioni associate
				LayoutInflater factory = LayoutInflater.from( this );
				View v = factory.inflate( R.layout.dialog_list_layout, (ViewGroup) findViewById( R.id.dialog_list_layout_root ) );

				if(booking_adapter == null)
					booking_adapter = new BookingAdapter( this, m_ombrellone.getBookings() );
				else{
					booking_adapter.setBookingsList( m_ombrellone.getBookings() );
					booking_adapter.notifyDataSetChanged();
				}

				listView = (ListView) v.findViewById( R.id.listView );
				listView.setAdapter( booking_adapter );

				//((LinearLayout.LayoutParams) listView.getLayoutParams()).height = (int)(Global.HEIGHT);
				//((LinearLayout.LayoutParams) listView.getLayoutParams()).width = (int)(Global.WIDTH - Global.sizewBox / 2);

				registerForContextMenu( listView );

				booking_list = new AlertDialog.Builder( this )
				.setTitle( R.string.list_bookings )
				.setView( v )
				.setPositiveButton( R.string.close, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						for(int i = booking_adapter.getCount() - 1; i >= 0; i--)
							booking_adapter.setEnabled( i, true );
					}
				} )
				.create();

				CheckBox box = (CheckBox) v.findViewById( R.id.checkbox_filter );
				box.setVisibility( CheckBox.VISIBLE );
				box.setOnCheckedChangeListener( this );

				button = (Button) v.findViewById( R.id.new_button );
				button.setText( R.string.add_booking );
				button.setOnClickListener( this );

				setAddButtonState();

				booking_list.show();
				booking_list.getWindow().setLayout( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT );
				//listView.setLayoutParams( new LinearLayout.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
				//listView.setMinimumHeight( (int)(Global.HEIGHT - Global.sizehBox) );

				break;

			case( 2 ):
				// visualizza i dettagli del posto selezionato
				factory = LayoutInflater.from( this );
				v = factory.inflate( R.layout.dialog_status_layout, (ViewGroup) findViewById( R.id.dialog_status_layout_root ) );

				final EditText name = (EditText) v.findViewById( R.id.name_place_edit );
				name.setText( m_ombrellone.getName() );
				final String initial_name = m_ombrellone.getName();
				if(!Global.edit_mode)
					name.setFocusable( false );
				else
					name.setFocusable( true );
				name.setOnClickListener( this );

				final EditText price = (EditText) v.findViewById( R.id.price_place_edit );
				price.setText( m_ombrellone.getPrice() + "" );
				final float initial_price = m_ombrellone.getPrice();

				new AlertDialog.Builder( this )
					.setTitle( getString( R.string.options ) + " " + m_ombrellone.getPosition() )
					.setView( v )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							String value = price.getText().toString();
							if(!value.equals( "" ))
								m_ombrellone.setPrice( Float.parseFloat( value ) );

							String name_position = name.getText().toString();
							if(!name_position.equals( "" ))
								m_ombrellone.setName( name_position );

							// controlla che i dati siano consistenti e che siano diversi da quelli iniziali
							if(!value.equals( "" ) && !name_position.equals( "" ) &&
									(!initial_name.equals( name_position ) || Float.parseFloat( value ) != initial_price))
								Global.net.sendMessage( SocketMsg.MODIFY_PLACE + "", m_ombrellone.getIDPlace() + "", name_position, value );

							Global.bm.setPressed( false );
						}
					} )
					.setNegativeButton( R.string.close, null )
					.create().show();

				break;

			case( 3 ):
				// modifica il prezzo dell'ombrellone (o degli ombrelloni)
				factory = LayoutInflater.from( this );
				View view = factory.inflate( R.layout.dialog_status_layout, (ViewGroup) findViewById( R.id.dialog_status_layout_root ) );

				((TextView) view.findViewById( R.id.name_text )).setVisibility( EditText.GONE );
				((EditText) view.findViewById( R.id.name_place_edit )).setVisibility( EditText.GONE );

				final EditText prezzo = (EditText) view.findViewById( R.id.price_place_edit );

				new AlertDialog.Builder( this )
					.setTitle( getString( R.string.options ) + " " + m_ombrellone.getPosition() )
					.setView( view )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							String p = prezzo.getText().toString();
							if(!p.equals( "" ))
								Global.bm.modifyPricePlaces( Float.parseFloat( p ) );
							else
								Global.bm.setPressed( false );
						}
					} )
					.setNegativeButton( R.string.close, null )
					.create().show();

				break;

			case( 4 ):
				// aggiunge posto
				m_ombrellone.setEditStatus( Ombrellone.INSERT );
				Global.bm.setPressed( false );

				break;

			case( 5 ):
				// cancella posto
				m_ombrellone.setEditStatus( Ombrellone.DELETE );
				Global.bm.setPressed( false );

				break;

			case( 6 ):
				// inserimento multiplo
				Global.bm.setAddPlaces();
				Global.bm.setPressed( false );

				break;

			case( 7 ):
				// cancellazione multipla
				Global.bm.deletePlaces();
				Global.bm.setPressed( false );

				break;
		}

		return super.onContextItemSelected( item );
    }

	@Override
	public void onClick( final View v )
	{
		switch( v.getId() ){
			case( R.id.connectionButton ):
				if(!Global.connected)
					login.show();
				else{
					// apre un dialog per essere sicuri di chiudere la comunicazione
					LayoutInflater factory = LayoutInflater.from( context );
					View view = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

					((TextView) view.findViewById( R.id.title_text )).setText( R.string.close_connection );

					new AlertDialog.Builder( context )
					.setTitle( R.string.attention )
					.setView( view )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							((Button) v).setText( R.string.connection );

							((EditText) findViewById( R.id.from_edit )).setVisibility( EditText.INVISIBLE );
							((EditText) findViewById( R.id.to_edit )).setVisibility( EditText.INVISIBLE );

							MyHandler.closedFromUser = true;
							Global.net.closeConnection();
							Global.bm.deleteAllPlaces();
							Global.logs.clear();

							Global.connected = false;

							if(Global.edit_mode){
								Global.edit_mode = false;
								((EditText) findViewById( R.id.rows_edit )).setVisibility( EditText.GONE );
								((EditText) findViewById( R.id.columns_edit )).setVisibility( EditText.GONE );
							}

							onCreateOptionsMenu( m_menu );
						}
					})
					.setNegativeButton( R.string.cancel, null )
					.create()
					.show();
				}

				break;

			case( R.id.from_edit ):
				int day, month, year;

				final EditText from = (EditText) findViewById( R.id.from_edit );
				String date = from.getText().toString();

				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3, 5 ) ) - 1;
				year = Integer.parseInt( date.substring( 6 ) );

				new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna, in quel caso mostra un errore
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((year < now.year) ||
						   (year == now.year && month < now.month) ||
						   (year == now.year && month == now.month && day < now.monthDay)){
							//createToast( R.string.date_selected_wrong, Gravity.CENTER, 0, 0, Toast.LENGTH_LONG );
							if(toast == null)
								toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							else
								toast.setText( R.string.date_selected_wrong );

							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// aggiorna la vista solo se la data e' diversa da quella precedente
						String date = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month + "-" + year;
						if(!date.equals( from.getText().toString() )){
							from.setText( date );

							EditText to = (EditText) findViewById( R.id.to_edit );
							String date_to = to.getText().toString();
							int t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
							int t_month = Integer.parseInt( date_to.substring( 3, 5 ) );
							int t_year = Integer.parseInt( date_to.substring( 6 ) );

							if((year > t_year) ||
								(year == t_year && month > t_month) ||
								(year == t_year && month == t_month && day > t_day)){
								to.setText( date );
								date_to = date;
							}

							Global.bm.updateView( date, date_to );
						}
					}

				}, year, month, day ).show();

				break;

			case( R.id.to_edit ):
				final EditText to = (EditText) findViewById( R.id.to_edit );
				date = to.getText().toString();

				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3, 5 ) ) - 1;
				year = Integer.parseInt( date.substring( 6 ) );

				new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna, in quel caso mostra un errore
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((year < now.year) ||
						   (year == now.year && month < now.month) ||
						   (year == now.year && month == now.month && day < now.monthDay)){
							if(toast == null)
								toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							else
								toast.setText( R.string.date_selected_wrong );

							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// aggiorna la vista solo se la data e' diversa da quella precedente
						String date = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month + "-" + year;
						if(!date.equals( to.getText().toString() )){
							to.setText( date );

							EditText from = (EditText) findViewById( R.id.from_edit );
							String date_from = from.getText().toString();
							int f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
							int f_month = Integer.parseInt( date_from.substring( 3, 5 ) );
							int f_year = Integer.parseInt( date_from.substring( 6 ) );

							if((year < f_year) ||
								(year == f_year && month < f_month) ||
								(year == f_year && month == f_month && day < f_day)){
								from.setText( date );
								date_from = date;
							}

							Global.bm.updateView( date_from, date );
						}
					}

				}, year, month, day ).show();

				break;

			case( R.id.new_button ):
				// nuova prenotazione o tariffa
				if(isRateOpen){
					RateActivity.type = RateActivity.CREATE;
					RateActivity.date_from = ((EditText) findViewById( R.id.from_edit )).getText().toString();
					RateActivity.date_to = ((EditText) findViewById( R.id.to_edit )).getText().toString();

					Intent i = new Intent( this, RateActivity.class );
					startActivity( i );
				}
				else{
					BookingActivity.addPlaces( null, m_ombrellone );
					BookingActivity.type = BookingActivity.CREATE;
					BookingActivity.date_from = ((EditText) findViewById( R.id.from_edit )).getText().toString();
					BookingActivity.date_to = ((EditText) findViewById( R.id.to_edit )).getText().toString();

					Intent i = new Intent( this, BookingActivity.class );
					startActivity( i );
				}

				break;

			case( R.id.rows_edit ):
				// apre un number picker creato manualmente in un XML
				LayoutInflater factory = LayoutInflater.from( context );
				View view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				final EditText rows = (EditText) findViewById( R.id.rows_edit );
				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( rows.getText() );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				isRowsEdit = true;

				new AlertDialog.Builder( context )
				.setTitle( R.string.rows )
				.setView( view )
				.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						String v = value.getText().toString();

						// controlla se e' stata inserita almeno una cifra
						if(value.equals( "" )){
							if(toast == null)
								toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
							else
								toast.setText( R.string.number );

							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();
						}
						else{
							// controlla se ci sono cambiamenti
							if(!rows.getText().toString().equals( v )){
								rows.setText( v );

								// aggiorna la vista
								int X = Integer.parseInt( ((EditText) findViewById( R.id.columns_edit )).getText().toString() );
								Global.bm.update_edit_view( activity, X, Integer.parseInt( v ) );
							}
						}
					}
				})
				.setNegativeButton( R.string.cancel, null )
				.create()
				.show();

				break;

			case( R.id.columns_edit ):
				// apre un number picker creato manualmente in un XML
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				final EditText columns = (EditText) findViewById( R.id.columns_edit );
				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( columns.getText() );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				isRowsEdit = false;

				new AlertDialog.Builder( context )
				.setTitle( R.string.columns )
				.setView( view )
				.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						String v = value.getText().toString();

						// controlla se e' stata inserita almeno una cifra
						if(value.equals( "" )){
							if(toast == null)
								toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
							else
								toast.setText( R.string.number );

							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();
						}
						else{
							// controlla se ci sono cambiamenti
							if(!columns.getText().toString().equals( v )){
								columns.setText( v );

								// aggiorna la vista
								int Y = Integer.parseInt( ((EditText) findViewById( R.id.rows_edit )).getText().toString() );
								Global.bm.update_edit_view( activity, Integer.parseInt( v ), Y );
							}
						}
					}
				})
				.setNegativeButton( R.string.cancel, null )
				.create()
				.show();

				break;

			case( R.id.total_cabins_edit ):
				// apre un number picker creato manualmente in un XML
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( cabins.getText() );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				new AlertDialog.Builder( context )
				.setTitle( R.string.cabin )
				.setView( view )
				.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						String val = value.getText().toString();

						// controlla se ci sono cambiamenti
						if(!val.equals( "" + Global.cabins )){
							// calcola quelle occupate
							int booked_cabins = Global.cabins - Global.free_cabins;

							int new_max_cabins = Integer.parseInt( val );
							if(new_max_cabins < booked_cabins){
								// TODO inserire le giuste stringhe, tipo:
								// 					il numero di cabine totale non puo' essere inferiore a quelle gia' occupate
								if(toast == null)
									toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
								else
									toast.setText( R.string.number );

								toast.setGravity( Gravity.CENTER, 0, 0 );
								toast.show();
							}

							Global.free_cabins = Global.free_cabins + (new_max_cabins - Global.cabins);

							((EditText) current_view.findViewById( R.id.free_cabins_edit )).setText( "" + Global.free_cabins );

							Global.cabins = Integer.parseInt( val );

							cabins.setText( val );

							// TODO inviare il nuovo dato al server?
							
						}
					}
				})
				.setNegativeButton( R.string.cancel, null )
				.create()
				.show();

				break;

			case( R.id.total_deckchairs_edit ):
				// apre un number picker creato manualmente in un XML
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				value = (EditText) view.findViewById( R.id.value_edit );
				value.setText( deckchairs.getText() );

				((ImageButton) view.findViewById( R.id.increase )).setOnClickListener( this );
				((ImageButton) view.findViewById( R.id.reduce )).setOnClickListener( this );

				new AlertDialog.Builder( context )
				.setTitle( R.string.deckchair )
				.setView( view )
				.setPositiveButton( R.string.set, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						String val = value.getText().toString();

						// controlla se ci sono cambiamenti
						if(!val.equals( "" + Global.deckchairs )){
							// calcola quelle occupate
							int booked_chairs = Global.deckchairs - Global.free_deckchairs;

							int new_max_chairs = Integer.parseInt( val );
							if(new_max_chairs < booked_chairs){
								// TODO inserire le giuste stringhe, tipo:
								// 					il numero di sdraio totale non puo' essere inferiore a quelle gia' occupate
								if(toast == null)
									toast = Toast.makeText( context, R.string.number, Toast.LENGTH_LONG );
								else
									toast.setText( R.string.number );

								toast.setGravity( Gravity.CENTER, 0, 0 );
								toast.show();
							}

							Global.free_deckchairs = Global.free_deckchairs + (new_max_chairs - Global.deckchairs);

							((EditText) current_view.findViewById( R.id.free_deckchairs_edit )).setText( "" + Global.free_deckchairs );

							Global.deckchairs = Integer.parseInt( val );

							deckchairs.setText( val );

							// TODO inviare il nuovo dato al server?
							
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

				int num = Integer.parseInt( value.getText().toString() );
				if(isRowsEdit && num < Global.MAX_ROWS || !isRowsEdit && num < Global.MAX_COLUMNS)
					value.setText( (num + 1) + "" );
				else{
					// mostra un avviso di essere oltre i limiti massimi della spiaggia
					if(toast == null)
						toast = Toast.makeText( context, (isRowsEdit) ? R.string.no_more_rows : R.string.no_more_columns, Toast.LENGTH_LONG );
					else
						toast.setText( (isRowsEdit) ? R.string.no_more_rows : R.string.no_more_columns );

					toast.setGravity( Gravity.CENTER, 0, (int)Global.sizehBox );
					toast.show();
				}

				break;

			case( R.id.reduce ):
				// riduce il numero di righe o colonne
				factory = LayoutInflater.from( context );
				view = factory.inflate( R.layout.number_picker_layout, (ViewGroup) findViewById( R.id.number_picker_layout_root ) );

				if(value == null)
					value = (EditText) view.findViewById( R.id.value_edit );

				if((num = Integer.parseInt( value.getText().toString() )) > 0)
					value.setText( (num - 1) + "" );

				break;

			case( R.id.name_place_edit ):
				if(!Global.edit_mode){
					if(toast == null)
						toast = Toast.makeText( context, R.string.no_selectable_name, Toast.LENGTH_LONG );
					else
						toast.setText( R.string.no_selectable_name );

					toast.setGravity( Gravity.CENTER, 0, 0 );
					toast.show();
				}

				break;
		}
	}

	@Override
	public boolean onMenuItemClick( MenuItem item )
	{
		switch( item.getItemId() ){
			case( 3 ):
				// modifica prenotazione
				Booking booking = m_ombrellone.getBooking( IDPrenotazione );
				if(booking != null){
			    	BookingActivity.addPlaces( null, m_ombrellone );
			    	BookingActivity.current_date_from = ((TextView) findViewById( R.id.from_edit )).getText().toString();
					BookingActivity.current_date_to = ((TextView) findViewById( R.id.to_edit )).getText().toString();
			    	BookingActivity.date_from = booking.getDateFrom();
					BookingActivity.date_to = booking.getDateTo();
					BookingActivity.IDPrenotazione = IDPrenotazione;
					BookingActivity.type = BookingActivity.MODIFY;

					Intent i = new Intent( context, BookingActivity.class );
					startActivity( i );
				}

				break;

			case( 4 ):
				// cancella prenotazione
				LayoutInflater factory = LayoutInflater.from( context );
				View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

				TextView text = (TextView) v.findViewById( R.id.title_text );
				text.setText( R.string.delete_booking_dialog );

				new AlertDialog.Builder( context )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						Booking booking = m_ombrellone.getBooking( IDPrenotazione );
						if(booking != null){
							int IDPlace = m_ombrellone.getIDPlace();

							Global.bm.deleteBooking( IDPlace, IDPrenotazione,
									((EditText) findViewById( R.id.from_edit )).getText().toString(),
									((EditText) findViewById( R.id.to_edit )).getText().toString());

							booking_adapter.notifyDataSetChanged();

							Global.net.sendMessage( SocketMsg.DELETE_BOOKING + "", IDPlace + "", IDPrenotazione + "",
													booking.getCabins() + "", booking.getDeckchairs() + "" );

							// riattiva il bottone per aggiungere prenotazioni
							setAddButtonState();
						}
					}
				})
				.setNegativeButton( "No", null )
				.create()
				.show();

				break;

			case( 5 ):
				// modifica tariffa
				Rate tariff = Global.bm.getRateByID( IDRate );
				if(tariff == null)
					break;

				RateActivity.type = RateActivity.MODIFY;
				RateActivity.date_from = tariff.getDateFrom();
				RateActivity.date_to = tariff.getDateTo();
				RateActivity.daily_price = tariff.getDailyPrice();
				RateActivity.weekly_price = tariff.getWeeklyPrice();
				RateActivity.IDRate = IDRate;

				Intent i = new Intent( this, RateActivity.class );
				startActivity( i );

				break;

			case( 6 ):
				// cancella tariffa
				factory = LayoutInflater.from( context );
				v = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

				((TextView) v.findViewById( R.id.title_text )).setText( R.string.delete_tariff_dialog );

				new AlertDialog.Builder( context )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						Global.bm.deleteRate( IDRate );

						rate_adapter.notifyDataSetChanged();

						Global.net.sendMessage( SocketMsg.DELETE_TARIFF + "", IDRate + "" );
					}
				})
				.setNegativeButton( "No", null )
				.create()
				.show();

				break;

			case( 7 ):
				// cancellazione file di log
				factory = LayoutInflater.from( context );
				v = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );

				((TextView) v.findViewById( R.id.title_text )).setText( R.string.delete_log_dialog );

				new AlertDialog.Builder( context )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int whichButton )
					{
						Global.removeLog( IDLog );
						log_adapter.notifyDataSetChanged();
					}
				})
				.setNegativeButton( "No", null )
				.create()
				.show();

				break;

			case( R.id.menu_cabin_deckchair_management ):
				// apre una finestra contenente la gestione di cabine e sdraie
				factory = LayoutInflater.from( activity );
				current_view = factory.inflate( R.layout.cabins_deckchairs, (ViewGroup) activity.findViewById( R.id.cabins_deckchairs_root ) );

				cabins = (EditText) current_view.findViewById( R.id.total_cabins_edit );
				cabins.setOnClickListener( this );
				cabins.setText( Global.cabins + "" );

				((EditText) current_view.findViewById( R.id.free_cabins_edit )).setText( Global.free_cabins + "" );

				final EditText price_cabin = (EditText) current_view.findViewById( R.id.price_cabins_edit );
				price_cabin.setText( Global.prezzo_cabina + "" );

				deckchairs = (EditText) current_view.findViewById( R.id.total_deckchairs_edit );
				deckchairs.setOnClickListener( this );
				deckchairs.setText( Global.deckchairs + "" );

				((EditText) current_view.findViewById( R.id.free_deckchairs_edit )).setText( Global.free_deckchairs + "" );

				final EditText price_deckchair = (EditText) current_view.findViewById( R.id.price_deckchairs_edit );
				price_deckchair.setText( Global.prezzo_sdraia + "" );

				final AlertDialog alert = new AlertDialog.Builder( activity )
				.setTitle( R.string.cabins_deckchair )
				.setView( current_view )
				.setPositiveButton( "Ok", null )
				.create();

				alert.setOnShowListener( new DialogInterface.OnShowListener(){
					@Override
					public void onShow( DialogInterface d )
					{
						Button b = alert.getButton( AlertDialog.BUTTON_POSITIVE );
						b.setOnClickListener( new View.OnClickListener(){
							@Override
							public void onClick( View view )
							{
								if(price_cabin.getText().toString().equals( "" )){
									if(toast == null)
										toast = Toast.makeText( context, R.string.price_cabin_error, Toast.LENGTH_LONG );
									else
										toast.setText( R.string.price_cabin_error );

									toast.setGravity( Gravity.CENTER, 0, 0 );
									toast.show();
								}
								else{
									if(price_deckchair.getText().toString().equals( "" )){
										if(toast == null)
											toast = Toast.makeText( context, R.string.price_deckchair_error, Toast.LENGTH_LONG );
										else
											toast.setText( R.string.price_deckchair_error );

										toast.setGravity( Gravity.CENTER, 0, 0 );
										toast.show();
									}
									else{
										// salva i valori e li invia al server
										int cabine = Integer.parseInt( cabins.getText().toString() );
										float prezzo_cabina = Float.parseFloat( price_cabin.getText().toString() );
										int sdraie = Integer.parseInt( deckchairs.getText().toString() );
										float prezzo_sdraia = Float.parseFloat( price_deckchair.getText().toString() );

										if(cabine != Global.cabins || sdraie != Global.deckchairs ||
										   prezzo_cabina != Global.prezzo_cabina || prezzo_sdraia != Global.prezzo_sdraia){
											Global.net.sendMessage( SocketMsg.MODIFY_DATA + "", cabine + "", prezzo_cabina + "",
																	sdraie + "", prezzo_sdraia + "" );

											// aggiorna i valori delle cabine e sdraie libere
											Global.free_cabins = Global.free_cabins + (Global.cabins - cabine);
											Global.free_deckchairs = Global.free_deckchairs + (Global.deckchairs - sdraie);

											Global.cabins = cabine;
											Global.deckchairs = sdraie;

											Global.prezzo_cabina = prezzo_cabina;
											Global.prezzo_sdraia = prezzo_sdraia;
										}

										alert.dismiss();
									}
								}
				            }
				        } );
				    }
				} );
				alert.show();

				break;

			case( R.id.menu_logs ):
				// apre la lista dei log
				factory = LayoutInflater.from( this );
				v = factory.inflate( R.layout.dialog_list_layout, (ViewGroup) findViewById( R.id.dialog_list_layout_root ) );

				if(log_list == null){
					log_adapter = new LogAdapter( this, Global.logs );

					//((LinearLayout.LayoutParams) listView.getLayoutParams()).height = (int)(Global.sizehBox * 100);

					log_list = new AlertDialog.Builder( this )
					.setTitle( R.string.list_logs )
					.setView( v )
					.setPositiveButton( R.string.close, new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							isLogOpen = false;
						}
					} )
					.create();
				}

				listView = (ListView) v.findViewById( R.id.listView );
				listView.setAdapter( log_adapter );

				registerForContextMenu( listView );

				isLogOpen = true;

				((Button) v.findViewById( R.id.new_button )).setVisibility( Button.GONE );
				((CheckBox) v.findViewById( R.id.checkbox_filter )).setVisibility( CheckBox.GONE );

				log_list.show();
				log_list.getWindow().setLayout( LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT );

				break;

			case( R.id.menu_settings ):
				// apre il menu' delle impostazioni
				i = new Intent( this, PrefActivity.class );
				startActivity( i );

				break;

			case( R.id.menu_select_all ):
				Global.bm.setPressed( true );
				break;

			case( R.id.menu_deselect_all ):
				Global.bm.setPressed( false );
				break;

			case( R.id.menu_help ):
				i = new Intent( this, HelpActivity.class );
				startActivity( i );

				break;
		}

		return false;
	}

	/** modifica lo stato del bottone del dialog in seguito a una cancellazione o una nuova prenotazione */
	private synchronized static void setAddButtonState()
	{
		if(button != null && m_ombrellone != null){
			if(m_ombrellone.getStatus() == Ombrellone.FREE)
				button.setEnabled( true );
			else
				button.setEnabled( false );
		}
	}

	@Override
	protected Dialog onCreateDialog( int id )
	{
		LayoutInflater factory = LayoutInflater.from( this );

		switch( id ){
			case( DIALOG_LOGIN ):
				final View v = factory.inflate( R.layout.dialog_login_layout, (ViewGroup) findViewById( R.id.dialog_login_layout_root ) );

				// gestore delle preferenze
				final SharedPreferences pref = getPreferences( MODE_PRIVATE );

				// carica un eventuale preferenza per l'username
				final EditText username = (EditText) v.findViewById( R.id.username_edit );
				username.setText( pref.getString( K_LOGIN, "" ) );
				if(username.getText().toString().length() > 0){
					EditText password = (EditText) v.findViewById( R.id.password_edit );
					password.requestFocus();

					CheckBox check = (CheckBox) v.findViewById( R.id.checkbox_remember );
					check.setChecked( true );
				}

				final EditText address = (EditText) v.findViewById( R.id.network_address_edit );
				address.setText( pref.getString( K_ADDRESS, "" ) );

				final AlertDialog alert = new AlertDialog.Builder( this )
					.setTitle( "Login" )
					.setView( v )
					.setPositiveButton( "Ok", null )
					.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener(){
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							EditText password = (EditText) v.findViewById( R.id.password_edit );
							password.setText( "" );

							CheckBox check = (CheckBox) v.findViewById( R.id.checkbox_remember );
							if(!check.isChecked()){
								Editor editor = pref.edit();
								editor.remove( K_LOGIN );
								editor.commit();
							}
						}
					} )
					.create();

				alert.setOnShowListener( new DialogInterface.OnShowListener(){
					@Override
					public void onShow( DialogInterface d )
					{
						// aggiunge l'event handler per il bottone OK
						Button b = alert.getButton( AlertDialog.BUTTON_POSITIVE );
						b.setOnClickListener( new View.OnClickListener(){
							@Override
							public void onClick( View view )
							{
								// controlla prima lo stato della rete
								int status = Global.net.getNetworkStatus( context );
								if(status == NetworkManager.NETWORK_CONNECTED){
									EditText password = (EditText) v.findViewById( R.id.password_edit );

									String addr = address.getText().toString();
									String user = username.getText().toString();
									String pwd = password.getText().toString();
									// tenta la connessione al server
									if(addr.length() > 0 && user.length() > 0 && pwd.length() > 0){
										dialog.setMessage( getString( R.string.connecting ) + "..." );
										dialog.setButton(
												DialogInterface.BUTTON_NEGATIVE, getString( R.string.cancel ),
												new DialogInterface.OnClickListener(){
													@Override
													public void onClick( DialogInterface d, int which )
													{
														DataReceiver.closeFromUser = true;
														Global.net.closeConnection();
														d.dismiss();
													}
												} );
										dialog.show();

										Global.net.setNetworkAddress( addr );
										Global.net.startConnection( user, pwd );

										Editor editor = pref.edit();

										editor.putString( K_ADDRESS, addr );

										CheckBox check = (CheckBox) v.findViewById( R.id.checkbox_remember );
										if(check.isChecked()){
											editor.putString( K_LOGIN, user );
											editor.commit();
										}
										else
											editor.remove( K_LOGIN ).commit();
									}
									else{
										if(toast == null)
											toast = Toast.makeText( context, R.string.insert_address_username_password, Toast.LENGTH_LONG );
										else
											toast.setText( R.string.insert_address_username_password );

										toast.setGravity( Gravity.CENTER, 0, 0 );
										toast.show();
									}
								}
								else{
									if(status == NetworkManager.NETWORK_DEVICES_OFF)
										onCreateDialog( NETWORK_OFF ).show();
									else
										onCreateDialog( NETWORK_DISCONNECTED ).show();
								}
				            }
				        });
				    }
				} );

				return alert;

			case( NETWORK_OFF ):
				View v1 = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );
				TextView text = (TextView) v1.findViewById( R.id.title_text );
				text.setText( R.string.network_off );
				AlertDialog alert1 = new AlertDialog.Builder( this )
					.setTitle( R.string.attention )
					.setView( v1 )
					.setNeutralButton( "Ok", null )
					.create();

				return alert1;

			case( NETWORK_DISCONNECTED ):
				v1 = factory.inflate( R.layout.dialog_layout, (ViewGroup) findViewById( R.id.dialog_layout_root ) );
				text = (TextView) v1.findViewById( R.id.title_text );
				text.setText( R.string.network_disconnect );
				alert1 = new AlertDialog.Builder( this )
					.setTitle( R.string.attention )
					.setView( v1 )
					.setNeutralButton( "Ok", null )
					.create();

				return alert1;
		}

		return null;
	}

	@Override
	public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
	{
		switch( buttonView.getId() ){
			case( R.id.checkbox_filter ):
				// abilita/disabilita gli elementi della prenotazione
				if(isChecked){
					for(int i = booking_adapter.getCount() - 1; i >= 0; i--){
						// controlla la data e li toglie
						Booking booking = booking_adapter.getItem( i );
						if(booking != null && !checkDates( booking.getDateFrom(), booking.getDateTo() ))
							booking_adapter.setEnabled( i, false );
					}
				}
				else{
					for(int i = booking_adapter.getCount() - 1; i >= 0; i--)
						booking_adapter.setEnabled( i, true );
				}

				booking_adapter.notifyDataSetChanged();

				break;
		}
	}

	/** controlla se la data della prenotazione interseca quella selezionata
	 * 
	 * @param from - data di inizio della prenotazione
	 * @param to - data di fine della prenotazione
	 * 
	 * @return TRUE se vi e' intersezione, FALSE altrimenti
	*/
	private boolean checkDates( String from, String to )
	{
		int l_f_day = Integer.parseInt( from.substring( 0, 2 ) );
		int l_f_month = Integer.parseInt( from.substring( 3, 5 ) );
		int l_f_year = Integer.parseInt( from.substring( 6 ) );

		int l_t_day = Integer.parseInt( to.substring( 0, 2 ) );
		int l_t_month = Integer.parseInt( to.substring( 3, 5 ) );
		int l_t_year = Integer.parseInt( to.substring( 6 ) );

		String date_from = ((EditText) findViewById( R.id.from_edit )).getText().toString();
		String date_to = ((EditText) findViewById( R.id.to_edit )).getText().toString();

		int c_f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
		int c_f_month = Integer.parseInt( date_from.substring( 3, 5 ) );
		int c_f_year = Integer.parseInt( date_from.substring( 6 ) );

		int c_t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
		int c_t_month = Integer.parseInt( date_to.substring( 3, 5 ) );
		int c_t_year = Integer.parseInt( date_to.substring( 6 ) );

		if((((l_f_year > c_f_year) ||
			(l_f_year == c_f_year && l_f_month > c_f_month) ||
			(l_f_year == c_f_year && l_f_month == c_f_month && l_f_day >= c_f_day)) &&
		   ((l_f_year < c_t_year) ||
			(l_f_year == c_t_year && l_f_month < c_t_month) ||
			(l_f_year == c_t_year && l_f_month == c_t_month && l_f_day <= c_t_day))) ||
		   (((l_t_year > c_f_year) ||
			(l_t_year == c_f_year && l_t_month > c_f_month) ||
			(l_t_year == c_f_year && l_t_month == c_f_month && l_t_day >= c_f_day)) &&
		   ((l_t_year < c_t_year) ||
			(l_t_year == c_t_year && l_t_month < c_t_month) ||
			(l_t_year == c_t_year && l_t_month == c_t_month && l_t_day <= c_t_day))) ||
		   (((l_f_year < c_f_year) ||
			(l_f_year == c_f_year && l_f_month < c_f_month) ||
			(l_f_year == c_f_year && l_f_month == c_f_month && l_f_day <= c_f_day)) &&
		   ((l_t_year > c_t_year) ||
			(l_t_year == c_t_year && l_t_month > c_t_month) ||
			(l_t_year == c_t_year && l_t_month == c_t_month && l_t_day >= c_t_day))))
			return true;

		return false;
	}

	/** crea un nuovo toast
	 * 
	 * @param textID - ID del testo da mostrare
	 * @param gravity - posizione dell'oggetto
	 * @param offsetX - offset X del toast
	 * @param offsetY - offset Y del toast
	 * @param duration - durata del toast
	*/
	/*private void createToast( int textID, int gravity, int offsetX, int offsetY, int duration )
	{
		LayoutInflater inflater = activity.getLayoutInflater();
		View v = inflater.inflate( R.layout.toast_layout, (ViewGroup) activity.findViewById( R.id.toast_layout_root ) );

		TextView text = (TextView) v.findViewById( R.id.text );
		text.setText( textID );
		text.setLayoutParams( new LinearLayout.LayoutParams( (int)(Global.sizewBox * 15), LayoutParams.WRAP_CONTENT ) );

		Toast toast = new Toast( activity );
		toast.setView( v );
		toast.setDuration( duration );
		toast.setGravity( gravity, offsetX, offsetY );
		toast.show();
	}*/

	/**
	 * Classe privata per il passaggio di parametri tra thread UI e non UI
	*/
	public static class MyHandler extends Handler implements OnClickListener, DialogInterface.OnClickListener
	{
		/* dialog per il download */
		private ProgressDialog download;
		/* dialog per le notifiche */
		private Dialog notify;
		/* testo per la visualizzazione della notifica */
		private TextView text;
		/* gestore delle notifiche */
		private static NotificationCompat.Builder builder;
		/* determina se e' stato chiuso dall'utente */
		public static boolean closedFromUser = false;

		public MyHandler()
		{
			LayoutInflater factory = LayoutInflater.from( activity );
			View v = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

			notify = new AlertDialog.Builder( activity )
				.setTitle( R.string.attention )
				.setView( v )
				.setPositiveButton( "Ok", this )
				.create();

			text = (TextView) v.findViewById( R.id.title_text );

			builder = new NotificationCompat.Builder( activity )
						.setSmallIcon( R.drawable.ic_launcher )
						.setAutoCancel( true );

			updateSettings();

			Intent notificationIntent = new Intent( activity, MainActivity.class );
			PendingIntent contentIntent = PendingIntent.getActivity( activity, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT );
			builder.setContentIntent( contentIntent ); 
		}

		@Override
		public void handleMessage( Message msg )
		{
			if(msg.what == SocketMsg.SERVER_DOWN){
				if(!notify.isShowing()){
					BookingActivity.close();

					if(booking_list != null && booking_list.isShowing())
						booking_list.dismiss();

					if(rate_list != null && rate_list.isShowing())
						rate_list.dismiss();

					if(closedFromUser)
						closedFromUser = false;
					else{
						text.setText( R.string.server_down );
						notify.show();

						((EditText) activity.findViewById( R.id.from_edit )).setVisibility( EditText.INVISIBLE );
						((EditText) activity.findViewById( R.id.to_edit )).setVisibility( EditText.INVISIBLE );
						((Button) activity.findViewById( R.id.connectionButton )).setText( R.string.connection );
					}

					Global.connected = false;

					activity.onCreateOptionsMenu( m_menu );

					if(download != null)
						download.dismiss();

					dialog.dismiss();

					// TODO se va via la connessione è corretto poter almeno vedere la spiaggia? per adesso sembrerebbe di no
					Global.bm.deleteAllPlaces();
					Global.bm.deleteAllRates();
				}

				return;
			}

			Bundle bundle = msg.getData();

			if(!Global.connected){
				dialog.dismiss();

				if(msg.what == SocketMsg.NO){
					showToast( ErrorMessage.getError( bundle.getStringArray( SocketMsg.NO + "" )[0].charAt( 0 ), activity ), Gravity.CENTER );

					LayoutInflater factory = LayoutInflater.from( activity );
					View v = factory.inflate( R.layout.dialog_login_layout, (ViewGroup) activity.findViewById( R.id.dialog_login_layout_root ) );
					EditText password = (EditText) v.findViewById( R.id.password_edit );
					password.setText( "" );
				}
				else{
					login.dismiss();
					Global.connected = true;

					String data[] = bundle.getStringArray( SocketMsg.OK + "" );

					Global.cabins = Global.free_cabins = Integer.parseInt( data[0] );
					Global.prezzo_cabina = Float.parseFloat( data[1] );
					Global.deckchairs = Global.free_deckchairs = Integer.parseInt( data[2] );
					Global.prezzo_sdraia = Float.parseFloat( data[3] );

					if(download == null){
						download = new ProgressDialog( activity );
						download.setTitle( activity.getString( R.string.download_data ) + "..." );
						download.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
						download.setCancelable( false );
					}

					if(!Global.isInBackground){
						download.setMax( Integer.parseInt( data[4] ) );
						download.show();
						download.setProgress( 0 );
					}

					Global.connected = true;

					Button connection = (Button) activity.findViewById( R.id.connectionButton );
					connection.setText( R.string.disconnection );
					connection.setOnClickListener( this );

					// necessario in caso di riavvio della connessione
					Global.bm.deleteAllPlaces();

					EditText from = (EditText) activity.findViewById( R.id.from_edit );
					from.setVisibility( EditText.VISIBLE );
					EditText to = (EditText) activity.findViewById( R.id.to_edit );
					to.setVisibility( EditText.VISIBLE );
				}
			}
			else{
				// gestione dati in arrivo
				switch( msg.what ){
					case( SocketMsg.ADD_BOOKING ):
						String data[] = bundle.getStringArray( SocketMsg.ADD_BOOKING + "" );
						int IDPlace = Integer.parseInt( data[0] );
						String position = Global.bm.getPosition( IDPlace );

						Global.bm.addBooking( IDPlace, Integer.parseInt( data[1] ), data[2], data[3], data[4], data[5],
											  data[6], Integer.parseInt(data[7] ), Integer.parseInt(data[8] ),
											  ((EditText) activity.findViewById( R.id.from_edit )).getText().toString(),
											  ((EditText) activity.findViewById( R.id.to_edit )).getText().toString(), data );

						if(!download.isShowing()){
							if(Global.isInBackground)
								addNotification( activity.getString( R.string.added_booking ),
												 activity.getString( R.string.added_booking_notification ) + " " + position,
												 SocketMsg.ADD_BOOKING );
							else{
								showToast( activity.getString( R.string.added_booking_upper ) + " " + position, Gravity.TOP | Gravity.LEFT );
								BookingActivity.updatePlaceStatus( false, IDPlace, false, false, false, 0 );
								addLog( activity.getString( R.string.added_booking_notification ) + " " + position );
							}
						}

						setAddButtonState();

						if(booking_adapter != null)
							booking_adapter.notifyDataSetChanged();

						break;

					case( SocketMsg.MODIFY_BOOKING ):
						data = bundle.getStringArray( SocketMsg.MODIFY_BOOKING + "" );
						IDPlace = Integer.parseInt( data[0] );
						position = Global.bm.getPosition( IDPlace );

						Global.bm.modifyBooking( IDPlace, Integer.parseInt( data[1] ), data[2], data[3], data[4], data[5], data[6],
												 Integer.parseInt( data[7] ), Integer.parseInt( data[8] ),
												 ((EditText) activity.findViewById( R.id.from_edit )).getText().toString(),
												 ((EditText) activity.findViewById( R.id.to_edit )).getText().toString(), data );

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.modified_booking ), 
											 activity.getString( R.string.modified_booking_notification ) + " " + position,
											 SocketMsg.MODIFY_BOOKING );
						else{
							showToast( activity.getString( R.string.modified_booking_upper ) + " " + position, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.modified_booking_notification ) + " " + position );
						}

						BookingActivity.updatePlaceStatus( false, IDPlace, false, false, false, 0 );

						if(booking_adapter != null)
							booking_adapter.notifyDataSetChanged();

						break;

					case( SocketMsg.DELETE_BOOKING ):
						data = bundle.getStringArray( SocketMsg.DELETE_BOOKING + "" );
						IDPlace = Integer.parseInt( data[0] );
						int IDPrenotazione = Integer.parseInt( data[1] );
						position = Global.bm.getPosition( IDPlace );

						Global.bm.deleteBooking( IDPlace, IDPrenotazione,
												 ((EditText) activity.findViewById( R.id.from_edit )).getText().toString(),
												 ((EditText) activity.findViewById( R.id.to_edit )).getText().toString() );

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.deleted_booking ),
											 activity.getString( R.string.deleted_booking_notification ) + " " + position,
											 SocketMsg.DELETE_BOOKING );
						else{
							showToast( activity.getString( R.string.deleted_booking_upper ) + " " + position, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.deleted_booking_notification ) + " " + position );
						}

						BookingActivity.updatePlaceStatus( false, IDPlace, false, false, true, IDPrenotazione );

						if(booking_adapter != null)
							booking_adapter.notifyDataSetChanged();

						break;

					case( SocketMsg.ADD_PLACE ):
						data = bundle.getStringArray( SocketMsg.ADD_PLACE + "" );
						EditText from = (EditText) activity.findViewById( R.id.from_edit );
						EditText to = (EditText) activity.findViewById( R.id.to_edit );
						Global.bm.addPlace( activity, from.getText().toString(), to.getText().toString(),
											  Integer.parseInt( data[0] ), Integer.parseInt( data[1] ), Integer.parseInt( data[2] ),
											  data[3], Float.parseFloat( data[4] ) );

						if(!download.isShowing()){
							if(Global.isInBackground)
								addNotification( activity.getString( R.string.added_place ),
												 activity.getString( R.string.added_place_notification ) + " " + data[1] + " - " + data[2],
												 SocketMsg.ADD_PLACE );
							else{
								showToast( activity.getString( R.string.added_place_upper ) + " " + data[1] + " - " + data[2],
																										Gravity.TOP | Gravity.LEFT );								
								addLog( activity.getString( R.string.added_place_notification ) + " " + data[1] + " - " + data[2] );
							}
						}
						else
							download.incrementProgressBy( 1 );

						break;

					case( SocketMsg.MODIFY_PLACE ):
						data = bundle.getStringArray( SocketMsg.MODIFY_PLACE + "" );
						IDPlace = Integer.parseInt( data[0] );
						position = Global.bm.getPosition( IDPlace );

						Global.bm.modifyPlace( IDPlace, data[1], Float.parseFloat( data[2] ) );

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.modified_place ),
									 		 activity.getString( R.string.modified_place_notification ) + " " + position,
									 		 SocketMsg.MODIFY_PLACE );
						else{
							showToast( activity.getString( R.string.modified_place_upper ) + " " + position, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.modified_place_notification ) + " " + position );
						}

						BookingActivity.updatePlaceStatus( false, IDPlace, false, false, false, 0 );

						break;

					case( SocketMsg.DELETE_PLACE ):
						data = bundle.getStringArray( SocketMsg.DELETE_PLACE + "" );
						IDPlace = Integer.parseInt( data[0] );
						position = Global.bm.getPosition( IDPlace );

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.deleted_place ),
									 		 activity.getString( R.string.deleted_place_notification ) + " " + position,
									 		 SocketMsg.DELETE_PLACE );
						else{
							showToast( activity.getString( R.string.deleted_place_upper ) + " " + position, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.deleted_place_notification ) + " " + position );
						}

						BookingActivity.updatePlaceStatus( false, IDPlace, true, false, false, 0 );

						Global.bm.deletePlace( IDPlace );

						break;

					case( SocketMsg.DELETE_ALL_PLACES ):
						Global.bm.deleteAllPlaces();

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.deleted_all_places_notification ), "", SocketMsg.DELETE_ALL_PLACES );
						else{
							showToast( activity.getString( R.string.deleted_all_places_upper ), Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.deleted_all_places_notification ) );
						}

						BookingActivity.updatePlaceStatus( false, 0, false, true, false, 0 );

						break;

					case( SocketMsg.OK ):
						// prenotazione/tariffa accettata
						if(isRateOpen){
							if(RateActivity.type == RateActivity.CREATE){
								RateActivity.close( Integer.parseInt( bundle.getStringArray( SocketMsg.OK + "" )[0] ) );

								if(rate_adapter != null){
									rate_adapter.notifyDataSetChanged();
									listView.smoothScrollToPosition( rate_adapter.getCount() - 1 );
								}

								showToast( activity.getString( R.string.tariff_accepted ), Gravity.CENTER );
							}
							else{
								RateActivity.close( 0 );

								if(rate_adapter != null)
									rate_adapter.notifyDataSetChanged();

								showToast( activity.getString( R.string.tariff_modified_accepted ), Gravity.CENTER );
							}
						}
						else{
							BookingActivity.check_response( true, bundle.getStringArray( SocketMsg.OK + "" ) );

							if(booking_adapter != null){
								booking_adapter.notifyDataSetChanged();
								if(BookingActivity.type == BookingActivity.CREATE)
								listView.smoothScrollToPosition( booking_adapter.getCount() - 1 );

								setAddButtonState();
							}

							Global.bm.setPressed( false );
						}

						break;

					case( SocketMsg.NO ):
						// prenotazione/tariffa rifiutata
						if(BookingActivity.isOpen())
							BookingActivity.check_response( false, bundle.getStringArray( SocketMsg.NO + "" ) );

						break;

					case( SocketMsg.ADD_TARIFF ):
						data = bundle.getStringArray( SocketMsg.ADD_TARIFF + "" );
						int IDRate = Integer.parseInt( data[0] );

						int index = Global.bm.addRate( IDRate, data[1], data[2],
														Float.parseFloat( data[3] ), Float.parseFloat( data[4] ) );

						if(rate_adapter != null)
							rate_adapter.notifyDataSetChanged();

						if(!download.isShowing()){
							if(Global.isInBackground)
								addNotification( activity.getString( R.string.added_tariff ),
												 activity.getString( R.string.added_tariff ) + " " + index,
												 SocketMsg.ADD_TARIFF );
							else{
								showToast( activity.getString( R.string.added_tariff_upper ) + " " + index, Gravity.TOP | Gravity.LEFT );
								addLog( activity.getString( R.string.added_tariff ) + " " + index );
							}
						}

						RateActivity.updateRateStatus( false, IDRate );

						if(rate_adapter != null){
							rate_adapter.notifyDataSetChanged();
							listView.smoothScrollToPosition( index );
						}

						break;

					case( SocketMsg.MODIFY_TARIFF ):
						data = bundle.getStringArray( SocketMsg.MODIFY_TARIFF + "" );
						IDRate = Integer.parseInt( data[0] );

						if((index = Global.bm.modifyRate( IDRate, data[1], data[2],
															Float.parseFloat( data[3] ), Float.parseFloat( data[4] ) )) == -1)
							return;

						if(rate_adapter != null)
							rate_adapter.notifyDataSetChanged();

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.modified_tariff ),
											 activity.getString( R.string.modified_tariff ) + " " + index,
											 SocketMsg.MODIFY_TARIFF );
						else{
							showToast( activity.getString( R.string.modified_tariff_upper ) + " " + index, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.modified_tariff ) + " " + index );
						}

						RateActivity.updateRateStatus( false, IDRate );

						if(rate_adapter != null)
							rate_adapter.notifyDataSetChanged();

						break;

					case( SocketMsg.DELETE_TARIFF ):
						data = bundle.getStringArray( SocketMsg.DELETE_TARIFF + "" );
						IDRate = Integer.parseInt( data[0] );

						if((index = Global.bm.deleteRate( IDRate )) == -1)
							return;

						if(rate_adapter != null)
							rate_adapter.notifyDataSetChanged();

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.deleted_tariff ),
											 activity.getString( R.string.deleted_tariff ) + " " + index,
											 SocketMsg.DELETE_TARIFF );
						else{
							showToast( activity.getString( R.string.deleted_tariff_upper ) + " " + index, Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.deleted_tariff ) + " " + index );
						}

						RateActivity.updateRateStatus( true, IDRate );

						break;

					case( SocketMsg.FINISH ):
						if(Global.edit_mode){
							dialog.dismiss();
							Global.edit_mode = false;
						}
						else
							download.dismiss();

						activity.onCreateOptionsMenu( m_menu );

						break;

					case( SocketMsg.MODIFY_DATA ):
						data = bundle.getStringArray( SocketMsg.MODIFY_DATA + "" );
						int cabine = Integer.parseInt( data[0] );
						int sdraie = Integer.parseInt( data[2] );

						Global.free_cabins = Global.free_cabins + (cabine - Global.cabins);
						Global.free_deckchairs = Global.free_deckchairs + (sdraie - Global.deckchairs);

						Global.cabins = cabine;
						Global.deckchairs = sdraie;

						Global.prezzo_cabina = Float.parseFloat( data[1] );
						Global.prezzo_sdraia = Float.parseFloat( data[3] );

						// aggiorna i valori correnti
						if(cabins != null && deckchairs != null){
							LayoutInflater factory = LayoutInflater.from( activity );
							View v = factory.inflate( R.layout.cabins_deckchairs,
													  (ViewGroup) activity.findViewById( R.id.cabins_deckchairs_root ) );

							cabins.setText( Global.cabins + "" );
							deckchairs.setText( Global.deckchairs + "" );

							((EditText) v.findViewById( R.id.free_cabins_edit )).setText( Global.free_cabins + "" );
							((EditText) v.findViewById( R.id.price_cabins_edit )).setText( Global.prezzo_cabina + "" );

							((EditText) v.findViewById( R.id.free_deckchairs_edit )).setText( Global.free_deckchairs + "" );
							((EditText) v.findViewById( R.id.price_deckchairs_edit )).setText( Global.prezzo_sdraia + "" );
						}

						if(m_ombrellone != null)
							BookingActivity.updatePlaceStatus( false, m_ombrellone.getIDPlace(), false, false, false, 0 );

						if(Global.isInBackground)
							addNotification( activity.getString( R.string.modified_data ),
											 activity.getString( R.string.modified_data_text ),
											 SocketMsg.MODIFY_DATA );
						else{
							showToast( activity.getString( R.string.modified_data ), Gravity.TOP | Gravity.LEFT );
							addLog( activity.getString( R.string.modified_data_text ) );
						}

						break;

					case( SocketMsg.ERROR_SERVER ):
						BookingActivity.close();

						if(booking_list != null && booking_list.isShowing())
							booking_list.dismiss();

						if(rate_list != null && rate_list.isShowing())
							rate_list.dismiss();

						if(log_list != null && log_list.isShowing())
							log_list.dismiss();

						Global.connected = false;

						activity.onCreateOptionsMenu( m_menu );

						// TODO se va via la connessione è corretto poter almeno vedere la spiaggia? per adesso sembrerebbe di no
						Global.bm.deleteAllPlaces();
						Global.bm.deleteAllRates();

						dialog.dismiss();

						((EditText) activity.findViewById( R.id.from_edit )).setVisibility( EditText.INVISIBLE );
						((EditText) activity.findViewById( R.id.to_edit )).setVisibility( EditText.INVISIBLE );
						((Button) activity.findViewById( R.id.connectionButton )).setText( R.string.connection );

						text.setText( R.string.error_server );
						notify.show();

						break;
				}
			}
		}

		/** mostra il toast dell'aggiornamento
		 * 
		 * @param text - il testo
		 * @param gravity - la posizione del toast
		*/
		private void showToast( String text, int gravity )
		{
			if(toast == null)
				toast = Toast.makeText( activity, text, Toast.LENGTH_LONG );
			else
				toast.setText( text );

			toast.setGravity( gravity, 0, 0 );
			toast.show();
		}

		/** crea una nuova notifica
		 * 
		 * @param title - il titolo
		 * @param text - il testo
		 * @param ID - l'ID associato alla notifica
		*/
		private void addNotification( String title, String text, int ID )
		{
			if(title != null)
				builder.setContentTitle( title );
			if(text != null)
				builder.setContentText( text );

			NotificationManager manager = (NotificationManager) activity.getSystemService( Context.NOTIFICATION_SERVICE );
			manager.notify( ID, builder.build() );

			setBadge( activity, ++count );
		}

		/** inserisce un nuovo log nella struttura
		 * 
		 * @param text - il testo del log
		*/
		private void addLog( String text )
		{
			Global.addLog( new Log( text, new Date().toString().substring( 0, 19 ) ) );

			if(log_adapter != null)
				log_adapter.notifyDataSetChanged();
		}

		/** aggiorna le impostazioni */
		public static void updateSettings()
		{
			int defaults = Notification.DEFAULT_LIGHTS;

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences( activity );
			if(sharedPrefs.getBoolean( activity.getString( R.string.key_vibration ), true ))
				defaults = defaults | Notification.DEFAULT_VIBRATE;

			if(sharedPrefs.getBoolean( activity.getString( R.string.key_audio ), true ))
				defaults = defaults | Notification.DEFAULT_SOUND;

			builder.setDefaults( defaults );
		}

		@Override
		public void onClick( final View v )
		{
			switch( v.getId() ){
				case( R.id.connectionButton ):
					if(!Global.connected)
						login.show();
					else{
						// apre un dialog per essere sicuri di chiudere la comunicazione
						LayoutInflater factory = LayoutInflater.from( activity );
						View view = factory.inflate( R.layout.dialog_layout, (ViewGroup) activity.findViewById( R.id.dialog_layout_root ) );

						((TextView) view.findViewById( R.id.title_text )).setText( R.string.close_connection );

						new AlertDialog.Builder( activity )
						.setTitle( R.string.attention )
						.setView( view )
						.setPositiveButton( "Ok", new DialogInterface.OnClickListener(){
							@Override
							public void onClick( DialogInterface dialog, int whichButton )
							{
								((Button) v).setText( R.string.connection );

								((EditText) activity.findViewById( R.id.from_edit )).setVisibility( EditText.INVISIBLE );
								((EditText) activity.findViewById( R.id.to_edit )).setVisibility( EditText.INVISIBLE );

								MyHandler.closedFromUser = true;
								Global.net.closeConnection();
								Global.bm.deleteAllPlaces();

								Global.connected = false;

								if(Global.edit_mode){
									Global.edit_mode = false;
									((EditText) activity.findViewById( R.id.rows_edit )).setVisibility( EditText.GONE );
									((EditText) activity.findViewById( R.id.columns_edit )).setVisibility( EditText.GONE );
								}

								activity.onCreateOptionsMenu( m_menu );
							}
						})
						.setNegativeButton( R.string.cancel, null )
						.create()
						.show();
					}

					break;
			}
		}

		@Override
		public void onClick( DialogInterface dialog, int whichButton )
		{
			notify.dismiss();
		}
	}
}