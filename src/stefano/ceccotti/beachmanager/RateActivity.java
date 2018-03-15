
package stefano.ceccotti.beachmanager;

import stefano.ceccotti.beachmanager.entities.Rate;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RateActivity extends ActionBarActivity implements OnClickListener
{
	/* determina il tipo di prenotazione */
	public static int type;
	/* ID della tariffa */
	public static int IDRate;
	/* dialog per le notifiche */
	private static Dialog notify;
	/* determina se l'activity e' attiva */
	private static boolean isOpen = false;
	/* data di inizio e fine periodo di prenotazione */
	public static String date_from, date_to;
	/* prezzo giornaliero e settimanale */
	public static float daily_price, weekly_price;
	/* il toast per le segnalazioni */
	private Toast toast;
	/* questa activity */
	private static Activity activity;

	/* il contesto dell'activity */
	private final Context context = this;
	/* tipo di prenotazione */
	public static final int MODIFY = 0, CREATE = 1;
	/* chavi per salvare lo stato */
	private static final String K_DAILY = "RATE_DAILY", K_WEEKLY = "RATE_WEEKLY", K_STATE = "RATE_STATE",
								K_DATE_FROM = "RATE_DATE_FROM", K_DATE_TO = "RATE_DATE_TO";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.rate_activity );

		activity = RateActivity.this;

		date_from = date_from.substring( 0, 5 );
		date_to = date_to.substring( 0, 5 );

		setOpen( true );

		TextView text = (TextView) findViewById( R.id.title_text );
		final EditText from = (EditText) findViewById( R.id.date_from_edit );
		final EditText to = (EditText) findViewById( R.id.date_to_edit );

		from.setText( date_from );
		from.setOnClickListener( this );
		from.setTextSize( 19 );
		from.setTextScaleX( Global.ratioW );
		to.setText( date_to );
		to.setOnClickListener( this );
		to.setTextSize( 19 );
		to.setTextScaleX( Global.ratioW );

		if(type == MODIFY)
			text.setText( R.string.modify_tariff );
		else{
			text.setText( R.string.new_tariff );
			IDRate = -1;
		}

		updateRateStatus( false, 0 );

		Button ok = (Button) findViewById( R.id.okButton );
		ok.setOnClickListener( this );

		Button cancel = (Button) findViewById( R.id.cancelButton );
		cancel.setOnClickListener( this );

		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable( new ColorDrawable( Color.parseColor( "#0099FF" ) ) );
		bar.setTitle( Html.fromHtml( "<font color=\"white\">" + getString( R.string.app_name ) + "</font>" ) );

		if(savedInstanceState != null){
			// ripristina lo stato
			((TextView) findViewById( R.id.daily_edit )).setText( savedInstanceState.getString( K_DAILY ) );
			((TextView) findViewById( R.id.weekly_edit )).setText( savedInstanceState.getString( K_WEEKLY ) );
			from.setText( savedInstanceState.getString( K_DATE_FROM ) );
			to.setText( savedInstanceState.getString( K_DATE_TO ) );

			String sts = savedInstanceState.getString( K_STATE );
			TextView status = (TextView) findViewById( R.id.status_value_text );
			status.setText( sts );
			if(sts.equals( getString( R.string.free ) ))
				status.setTextColor( Color.GREEN );
			else
				status.setTextColor( Color.RED );
		}

		getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_rate, menu );

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
	protected void onSaveInstanceState( Bundle state )
	{
		EditText daily = (EditText) findViewById( R.id.daily_edit );
		state.putString( K_DAILY, daily.getText().toString() );

		EditText weekly = (EditText) findViewById( R.id.weekly_edit );
		state.putString( K_WEEKLY, weekly.getText().toString() );

		EditText from = (EditText) findViewById( R.id.date_from_edit );
		state.putString( K_DATE_FROM, from.getText().toString() );

		EditText to = (EditText) findViewById( R.id.date_to_edit );
		state.putString( K_DATE_TO, to.getText().toString() );

		TextView status = (TextView) findViewById( R.id.status_value_text );
		state.putString( K_STATE, status.getText().toString() );

		super.onSaveInstanceState( state );
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

		TextView status = (TextView) findViewById( R.id.status_value_text );
		status.setTextSize( 19 );
		status.setTextScaleX( Global.ratioW );
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

		super.onDestroy();
	}

	@Override
	public void onClick( View v )
	{
		switch( v.getId() ){
			case( R.id.okButton ):
				String daily_price_edit = ((EditText) findViewById( R.id.daily_edit )).getText().toString();
				if(daily_price_edit.equals( "" )){
					showToast( R.string.daily_error, Gravity.CENTER );
					((EditText) findViewById( R.id.daily_edit )).requestFocus();
					return;
				}

				String weekly_price_edit = ((EditText) findViewById( R.id.weekly_edit )).getText().toString();
				if(weekly_price_edit.equals( "" )){
					showToast( R.string.weekly_error, Gravity.CENTER );
					((EditText) findViewById( R.id.weekly_edit )).requestFocus();
					return;
				}

				String date_from_edit = ((EditText) findViewById( R.id.date_from_edit )).getText().toString();
				String date_to_edit = ((EditText) findViewById( R.id.date_to_edit )).getText().toString();

				if(type == CREATE){
					Global.net.sendMessage( SocketMsg.ADD_TARIFF + "", date_from_edit, date_to_edit, daily_price_edit, weekly_price_edit );
					daily_price = Float.parseFloat( daily_price_edit );
					weekly_price = Float.parseFloat( weekly_price_edit );
				}
				else{
					// se i dati sono cambiati invia le modifiche
					if(!date_from.equals( date_from_edit ) || !date_to.equals( date_to_edit ) ||
					   !daily_price_edit.equals( daily_price ) || !weekly_price_edit.equals( weekly_price )){
						Global.net.sendMessage( SocketMsg.MODIFY_TARIFF + "", date_from_edit, date_to_edit, daily_price_edit, weekly_price_edit );

						daily_price = Integer.parseInt( daily_price_edit );
						weekly_price = Integer.parseInt( weekly_price_edit );
					}
				}

				break;

			case( R.id.cancelButton ):
				finish();
				break;

			case( R.id.date_from_edit ):
				int day, month;
				final EditText from = (EditText) findViewById( R.id.date_from_edit );
				String date = from.getText().toString();
				Time now = new Time();
				now.setToNow();

				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3 ) ) - 1;

				new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((month < now.month) ||
						   (month == now.month && day < now.monthDay)){
							Toast toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// controlla se la data e' cambiata
						String c_date_from = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month;
						if(!c_date_from.equals( from.getText().toString() )){
							from.setText( c_date_from );

							EditText to = (EditText) findViewById( R.id.date_to_edit );
							String c_date_to = to.getText().toString();
							int t_day = Integer.parseInt( c_date_to.substring( 0, 2 ) );
							int t_month = Integer.parseInt( c_date_to.substring( 3 ) );

							if((month > t_month) ||
							   (month == t_month && day > t_day)){
								to.setText( c_date_from );
								c_date_to = c_date_from;
							}

							updateRateStatus( false, IDRate );
						}
					}

				}, now.year, month, day ).show();

				break;

			case( R.id.date_to_edit ):
				final EditText to = (EditText) findViewById( R.id.date_to_edit );
				date = to.getText().toString();
				now = new Time();

				now.setToNow();
				now.month++;

				day = Integer.parseInt( date.substring( 0, 2 ) );
				month = Integer.parseInt( date.substring( 3 ) ) - 1;

				DatePickerDialog dialog = new DatePickerDialog( context, new OnDateSetListener(){
					@Override
					public void onDateSet( DatePicker datepicker, int year, int month, int day )
					{
						month++;

						// controlla se la data e' antecedente a quella odierna
						Time now = new Time();
						now.setToNow();
						now.month++;

						if((month < now.month) ||
						   (month == now.month && day < now.monthDay)){
							Toast toast = Toast.makeText( context, R.string.date_selected_wrong, Toast.LENGTH_LONG );
							toast.setGravity( Gravity.CENTER, 0, 0 );
							toast.show();

							return;
						}

						// controlla se la data e' cambiata
						String c_date_to = ((day < 10) ? "0" : "") + day + "-" + ((month < 10) ? "0" : "") + month;
						if(!c_date_to.equals( to.getText().toString() )){
							to.setText( c_date_to );

							EditText from = (EditText) findViewById( R.id.date_from_edit );
							String c_date_from = from.getText().toString();
							int f_day = Integer.parseInt( c_date_from.substring( 0, 2 ) );
							int f_month = Integer.parseInt( c_date_from.substring( 3 ) );

							if((month < f_month) ||
							   (month == f_month && day < f_day)){
								from.setText( c_date_to );
								c_date_from = c_date_to;
							}

							updateRateStatus( false, IDRate );
						}
					}

				}, now.year, month, day );

				dialog.show();

				break;
		}
	}

	/** mostra il toast dell'aggiornamento
	 * 
	 * @param textID - ID del testo
	 * @param gravity - la posizione del toast
	*/
	private void showToast( int textID, int gravity )
	{
		if(toast == null)
			toast = Toast.makeText( context, "", Toast.LENGTH_LONG );

		toast.setText( textID );
		toast.setGravity( gravity, 0, 0 );
		toast.show();
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

	/** aggiorna lo stato della tariffa
	 * 
	 * @param delete - TRUE se la prenotazione e' stata cancellata, FALSE altrimenti
	 * @param ID - ID della tariffa cancellata
	*/
	public static synchronized void updateRateStatus( boolean delete, int ID )
	{
		if(isOpen){
			if(delete && ID == IDRate){
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

					((TextView) v.findViewById( R.id.title_text )).setText( R.string.deleted_out_rate );
				}

				notify.show();

				activity.finish();
			}

			int stato = Global.bm.check_rate_status( IDRate,
													 ((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString(),
													 ((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString() );

			if(stato == Rate.FREE){
				((Button) activity.findViewById( R.id.okButton )).setEnabled( true );
				TextView status = (TextView) activity.findViewById( R.id.status_value_text );
				status.setTextSize( 19 );
				status.setTextScaleX( Global.ratioW );
				status.setTextColor( Color.GREEN );
				status.setText( R.string.free );
			}
			else{
				TextView status = (TextView) activity.findViewById( R.id.status_value_text );
				status.setTextSize( 19 );
				status.setTextScaleX( Global.ratioW );
				status.setTextColor( Color.RED );
				status.setText( R.string.occupied );

				String c_date_from = ((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString();
				String c_date_to = ((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString();
				if(type == CREATE || (type == MODIFY && (!c_date_from.equals( date_from ) || !c_date_to.equals( date_to ))))
					((Button) activity.findViewById( R.id.okButton )).setEnabled( false );
				else
					((Button) activity.findViewById( R.id.okButton )).setEnabled( true );
			}

			// inserisce/modifica i parametri della tariffa
			if(type == MODIFY){
				((EditText) activity.findViewById( R.id.daily_edit )).setText( daily_price + "" );
				((EditText) activity.findViewById( R.id.weekly_edit )).setText( weekly_price + "" );
			}
		}
	}
	

	/** chiude l'activity
	 * 
	 * @param ID - ID della prenotazione
	*/
	public static void close( int ID )
	{
		String date_from = ((EditText) activity.findViewById( R.id.date_from_edit )).getText().toString().substring( 0, 5 );
		String date_to = ((EditText) activity.findViewById( R.id.date_to_edit )).getText().toString().substring( 0, 5 );

		// inserisce o modifica la tariffa
		if(type == CREATE)
			Global.bm.addRate( ID, date_from, date_to, daily_price, weekly_price );
		else
			Global.bm.modifyRate( IDRate, date_from, date_to, daily_price, weekly_price );

		activity.finish();
	}
}