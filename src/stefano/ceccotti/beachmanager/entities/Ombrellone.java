
package stefano.ceccotti.beachmanager.entities;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.R;
import stefano.ceccotti.beachmanager.utils.Global;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;

public class Ombrellone implements OnClickListener, AnimationListener
{
	/* identificativo del posto */
	private int IDPlace;
	/* nome/posizione dell'ombrellone */
	private String name = "0", tmp_name = "0";
	/* il bottone associato */
	private ImageButton button;
	/* coordinate del posto */
	private int x, y;
	/* valore associato al posto */
	private float price;
	/* l'utente che ha prenotato il posto */
	private ArrayList<Booking> bookings = null;
	/* stato dell'ombrellone */
	private int status, edit_status;
	/* determina lo stato del bottone */
	private boolean pressed = false;
	/* contatore delle animazioni */
	private int counter = 0;
	/* immagini dell'ombrellone verde, rosso e invisibile */
	//private static Drawable green, red, invisible;

	/* le animazioni del bottone */
	private final ScaleAnimation GROW = new ScaleAnimation( 0.5f, 1,
															0.5f, 1,
															Animation.RELATIVE_TO_SELF, 0.5f,
															Animation.RELATIVE_TO_SELF, 0.5f ),
								 REDUCE = new ScaleAnimation( 1, 0.7f,
															  1, 0.7f,
															  Animation.RELATIVE_TO_SELF, 0.5f,
															  Animation.RELATIVE_TO_SELF, 0.5f );
	/* stato dell'ombrellone */
	public static final int FREE = 0, OCCUPIED = 1, NULL = 2;
	/* stato dell'ombrellone durante l'edit mode */
	public static final int OK = 3, EMPTY = 4, INSERT = 5, NEW_INSERT = 6, OLD_INSERT = 7, DELETE = 8, NEW_DELETE = 9, OLD_DELETE = 10;
	

	public Ombrellone( ImageButton button, int x, int y )
	{
		IDPlace = -1;
		price = 0;

		this.x = x;
		this.y = y;

		bookings = new ArrayList<Booking>();

		status = NULL;

		this.button = button;
		//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, invisible );
		button.setImageResource( R.drawable.trasparenza );
		button.setVisibility( ImageButton.INVISIBLE );
		button.setScaleType( ImageButton.ScaleType.FIT_CENTER );
		button.setTag( getPosition() );
		button.setOnClickListener( this );

		GROW.setAnimationListener( this );
		GROW.setDuration( 350 );
		REDUCE.setAnimationListener( this );
		REDUCE.setDuration( 200 );

		if(Global.edit_mode)
			edit_status = EMPTY;
		else
			edit_status = OK;
	}

	/** inizializza le strutture dati */
	/*public static void init( Activity activity )
	{
		//green = activity.getResources().getDrawable( R.drawable.ombrellone_verde );
		//red = activity.getResources().getDrawable( R.drawable.ombrellone_rosso );
		//invisible = activity.getResources().getDrawable( R.drawable.trasparenza );
	}*/

	/** ricostruisce il bottone
	 * 
	 * @param button - il nuovo bottone
	*/
	public void re_build( ImageButton button )
	{
		if(IDPlace == -1)
			button.setImageResource( R.drawable.trasparenza );
			//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, invisible );
		else{
			if(status == FREE)
				button.setImageResource( R.drawable.ombrellone_verde );
				//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, green );
			else
				button.setImageResource( R.drawable.ombrellone_rosso );
				//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, red );
		}

		button.setId( this.button.getId() );
		button.setVisibility( (IDPlace == -1) ? ImageButton.INVISIBLE : this.button.getVisibility() );
		button.setScaleType( ImageButton.ScaleType.FIT_CENTER );
		button.setTag( getPosition() );
		button.setOnClickListener( this );
		if(pressed)
			button.getBackground().setColorFilter( Color.CYAN, Mode.MULTIPLY );

		this.button = button;
	}

	@Override
	public void onAnimationStart( Animation animation )
	{
		
	}

	@Override
	public void onAnimationRepeat( Animation animation )
	{
		
	}

	@Override
	public void onAnimationEnd( Animation animation )
	{
		if(++counter == 1)
			button.startAnimation( REDUCE );
	}

	/** aggiorna lo stato in base al nuovo periodo
	 * 
	 * @param c_f_day - giorno data inizio
	 * @param c_f_month - mese data inizio
	 * @param c_f_year - anno data inizio
	 * @param c_t_day - giorno data fine
	 * @param c_t_month - mese data fine
	 * @param c_t_year - anno data fine
	*/
	public void updateStatus( int c_f_day, int c_f_month, int c_f_year, int c_t_day, int c_t_month, int c_t_year )
	{
		if(IDPlace == -1)
			return;

		int l_f_day, l_f_month, l_f_year;
		int l_t_day, l_t_month, l_t_year;

		for(int i = bookings.size() - 1; i >= 0; i--){
			String date_from = bookings.get( i ).getDateFrom();
			String date_to = bookings.get( i ).getDateTo();

			l_f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
			l_f_month = Integer.parseInt( date_from.substring( 3, 5 ) );
			l_f_year = Integer.parseInt( date_from.substring( 6 ) );

			l_t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
			l_t_month = Integer.parseInt( date_to.substring( 3, 5 ) );
			l_t_year = Integer.parseInt( date_to.substring( 6 ) );

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
				(l_t_year == c_t_year && l_t_month == c_t_month && l_t_day >= c_t_day)))){
				if(status == FREE){
					button.setImageResource( R.drawable.ombrellone_rosso );
					//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, red );
					status = OCCUPIED;
				}

				return;
			}
		}

		if(status == OCCUPIED){
			button.setImageResource( R.drawable.ombrellone_verde );
			//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, green );
			status = FREE;
		}
	}

	/** restituisce lo stato dell'ombrellone
	 * 
	 * @return lo stato
	*/
	public int getStatus()
	{
		return status;
	}

	/** restituisce lo stato dell'ombrellone durante l'edit
	 * 
	 * @return lo stato dell'edit
	*/
	public int getEditStatus()
	{
		return edit_status;
	}

	/** restituisce lo stato (FREE / OCCUPIED) dell'ombrellone in base al periodo richiesto
	 * 
	 * @param IDPrenotazione - ID della prenotazione da evitare di considerare (-1 se non ce ne sono)
	 * @param from - data di inizio periodo
	 * @param to - data di fine periodo
	 * 
	 * @return lo stato
	*/
	public int checkStatus( int IDPrenotazione, String from, String to )
	{
		int c_f_day = Integer.parseInt( from.substring( 0, 2 ) );
		int c_f_month = Integer.parseInt( from.substring( 3, 5 ) );
		int c_f_year = Integer.parseInt( from.substring( 6 ) );

		int c_t_day = Integer.parseInt( to.substring( 0, 2 ) );
		int c_t_month = Integer.parseInt( to.substring( 3, 5 ) );
		int c_t_year = Integer.parseInt( to.substring( 6 ) );

		int l_f_day, l_f_month, l_f_year;
		int l_t_day, l_t_month, l_t_year;

		for(int i = bookings.size() - 1; i >= 0; i--){
			if(bookings.get( i ).getIDPrenotazione() != IDPrenotazione){
				String date_from = bookings.get( i ).getDateFrom();
				String date_to = bookings.get( i ).getDateTo();
	
				l_f_day = Integer.parseInt( date_from.substring( 0, 2 ) );
				l_f_month = Integer.parseInt( date_from.substring( 3, 5 ) );
				l_f_year = Integer.parseInt( date_from.substring( 6 ) );
	
				l_t_day = Integer.parseInt( date_to.substring( 0, 2 ) );
				l_t_month = Integer.parseInt( date_to.substring( 3, 5 ) );
				l_t_year = Integer.parseInt( date_to.substring( 6 ) );
	
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
					return OCCUPIED;
			}
		}

		return FREE;
	}

	/** restituisce la posizione del posto
	 * 
	 * @return la posizione
	*/
	public synchronized String getPosition()
	{
		return y + " - " + x;
	}

	/** restituisce la coordinata X
	 * 
	 * @return la coordinata X
	*/
	public int getX()
	{
		return x;
	}

	/** restituisce la coordinata Y
	 * 
	 * @return la coordinata Y
	*/
	public int getY()
	{
		return y;
	}

	/** restituisce l'ID del posto
	 * 
	 * @return l'ID del posto
	*/
	public int getIDPlace()
	{
		return IDPlace;
	}

	/** restituisce il bottone associato
	 * 
	 * @return il bottone
	*/
	public ImageButton getButton()
	{
		return button;
	}

	/** modifica la visibilita' del posto
	 * 
	 * @param visible - TRUE se e' visibile, FALSE altrimenti
	 * @param IDPlace - ID da assegnare (-1 in caso venga reso invisibile)
	 * @param name - nome dell'ombrellone
	 * @param price - valore dell'ombrellone
	*/
	public void setVisibility( boolean visible, int IDPlace, String name, float price )
	{
		this.IDPlace = IDPlace;

		if(visible){
			button.setId( IDPlace );
			button.setImageResource( R.drawable.ombrellone_verde );
			//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, green );
			button.setVisibility( ImageButton.VISIBLE );
			button.startAnimation( GROW );
			this.name = name;
			this.price = price;
			status = FREE;
			edit_status  = OK;
		}
		else{
			button.setVisibility( ImageButton.INVISIBLE );
			bookings.clear();
			status = NULL;
			button.getBackground().setColorFilter( null );
			this.name = "0";
			this.tmp_name = "0";
			this.price = 0;
		}
	}

	/** modifica lo stato dell'ombrellone
	 * 
	 * @param status - il nuovo stato
	*/
	public void setEditStatus( int status )
	{
		if(status == INSERT){
			switch( edit_status ){
				case( OK ):
					status = OLD_INSERT;
					break;
				case( OLD_DELETE ):
					status = OK;
					break;
				default:
					status = NEW_INSERT;
					break;
			}
		}

		if(status == DELETE){
			switch( edit_status ){
				case( OK ):
					status = OLD_DELETE;
					break;
				case( OLD_INSERT ):
					status = OK;
					break;
				default:
					status = NEW_DELETE;
					break;
			}
		}

		switch( edit_status = status ){
			case( OK ):
				if(IDPlace == -1)
					button.setImageResource( R.drawable.trasparenza );
					//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, green );
				else{
					if(this.status == FREE)
						button.setImageResource( R.drawable.ombrellone_verde );
						//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, green );
					else
						button.setImageResource( R.drawable.ombrellone_rosso );
						//button.setCompoundDrawablesWithIntrinsicBounds( null, null, null, red );
				}

				break;

			case( EMPTY ):
				button.setImageResource( R.drawable.trasparenza );
				button.setVisibility( Button.VISIBLE );

				break;

			case( NEW_INSERT ):
				button.setImageResource( R.drawable.ombrellone_verde );

				break;

			case( OLD_INSERT ):
				if(IDPlace == -1 || this.status == FREE)
					button.setImageResource( R.drawable.ombrellone_verde );
				else
					button.setImageResource( R.drawable.ombrellone_rosso );

				break;

			case( NEW_DELETE ):
				button.setImageResource( R.drawable.trasparenza );

				break;

			case( OLD_DELETE ):
				button.setImageResource( R.drawable.trasparenza );

				break;
		}
	}

	/** inserisce la modalità edit della spiaggia
	 * 
	 * @param edit_mode - TRUE se e' in modalita' edit, FALSE altrimenti
	*/
	public void setEditMode( boolean edit_mode )
	{
		if(edit_mode){
			if(IDPlace == -1)
				button.setVisibility( Button.VISIBLE );
		}
		else{
			if(IDPlace == -1){
				button.setImageResource( R.drawable.trasparenza );
				button.setVisibility( Button.INVISIBLE );
			}
			else{
				if(status == FREE)
					button.setImageResource( R.drawable.ombrellone_verde );
				else
					button.setImageResource( R.drawable.ombrellone_rosso );

				button.setVisibility( Button.VISIBLE );
			}

			edit_status = OK;

			pressed = false;
			button.getBackground().setColorFilter( null );
		}
	}

	@Override
	public void onClick( View v )
	{
		pressed = !pressed;

		if(pressed)
			button.getBackground().setColorFilter( Color.CYAN, Mode.MULTIPLY );
		else
			button.getBackground().setColorFilter( null );
	}

	/** determina se il bottone e' premuto
	 * 
	 * @return TRUE se il bottone e' premuto, FALSE altrimenti
	*/
	public boolean isPressed()
	{
		return pressed;
	}

	/** modifica lo stato del bottone
	 * 
	 * @param TRUE se il bottone e' premuto, FALSE altrimenti
	*/
	public void setPressed( boolean pressed )
	{
		if(this.pressed = pressed)
			button.getBackground().setColorFilter( Color.CYAN, Mode.MULTIPLY );
		else
			button.getBackground().setColorFilter( null );
	}

	/** assegna un valore al posto
	 * 
	 * @param price - il valore
	*/
	public void setPrice( float price )
	{
		this.price = price;
	}

	/** restituisce il valore del posto
	 * 
	 * @return il valore
	*/
	public float getPrice()
	{
		return price;
	}

	/** assegna un nome al posto
	 * 
	 * @param name - il nome
	*/
	public void setName( String name )
	{
		this.name = name;
	}

	/** assegna un nome povvisorio al posto
	 * 
	 * @param name - il nome temporaneo
	*/
	public void setTmpName( String name )
	{
		tmp_name = name;
	}

	/** restituisce il nome del posto
	 * 
	 * @return il nome
	*/
	public String getName()
	{
		return name;
	}

	/** restituisce il nome provvisorio del posto
	 * 
	 * @return il nome provvisorio
	*/
	public String getTmpName()
	{
		return tmp_name;
	}

	/** determina se il nome del bottone e' un numero
	 * 
	 * @return TRUE se il nome e' un numero, FALSE altrimenti
	*/
	public boolean isNameNumber()
	{
		// controlla usando il try e catch
		try{
			Integer.parseInt( name );
		}
		catch( Exception e ){ return false; }

		return true;
	}

	/** determina se il nome provvisorio del bottone e' un numero
	 * 
	 * @return TRUE se il nome e' un numero, FALSE altrimenti
	*/
	public boolean isTmpNameNumber()
	{
		// controlla usando il try e catch
		try{
			Integer.parseInt( tmp_name );
		}
		catch( Exception e ){ return false; }

		return true;
	}

	/** controlla se il nome provvisorio e il nome effettivo sono uguali
	 * 
	 * @return TRUE se sono uguali, FALSE altrimenti
	*/
	public boolean checkName()
	{
		if(name.equals( tmp_name ))
			return true;
		else{
			name = new String( tmp_name );
			return false;
		}
	}

	/** restituisce la lista di prenotazioni
	 * 
	 * @return la lista
	*/
	public ArrayList<Booking> getBookings()
	{
		return bookings;
	}

	/** restituisce il prezzo dell'ombrellone
	 * 
	 * @param IDPrenotazione - ID della prenotazione
	 * 
	 * @return il prezzo
	*/
	public synchronized float getPrice( int IDPrenotazione )
	{
		return getBooking( IDPrenotazione ).getPrice();
	}

	/** controlla se le coordinate coincidono
	 * 
	 * @param x - la coordinata X
	 * @param y - la coordinata Y
	 * 
	 * @return TRUE se le coordinate coincidono, FALSE altrimenti
	*/
	public boolean checkPosition( int x, int y )
	{
		return (this.x == x && this.y == y);
	}

	/** modifica i valori dell'ombrellone
	 * 
	 * @param name - il nuovo nome
	 * @param price - il nuovo prezzo
	*/
	public void modifyValues( String name, float price )
	{
		this.name = name;
		this.price = price;
	}

	/** restituisce la prenotazione richiesta
	 * 
	 * @param index - l'indice della prenotazione richiesta
	 * 
	 * @return la prenotazione, se esiste, NULL altrimenti
	*/
	public synchronized Booking getBooking( int IDPrenotazione )
	{
		for(int i = bookings.size() - 1; i >= 0; i--){
			if(bookings.get( i ).getIDPrenotazione() == IDPrenotazione)
				return bookings.get( i );
		}

		return null;
	}

	/** inserisce una nuova prenotazione
	 * 
	 * @param IDPrenotazione - ID associato alla prenotazione
	 * @param from - data di inizio periodo
	 * @param to - data di fine periodo
	 * @param date_from - data di inizio della nuova prenotazione
	 * @param date_to - data di fine della nuova prenotazione
	 * @param name - nome di chi ha effettuato la prenotazione
	 * @param surname - cognome di chi ha effettuato la prenotazione
	 * @param phone - numero di telefono di chi ha effettuato la prenotazione
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	 * @param price - il prezzo
	 * @param current_date_from - data di inizio periodo selezionata
	 * @param current_date_to - data di fine periodo selezionata
	 * @param IDCabins - lista degli ID delle cabine
	*/
	public synchronized void addBooking( int IDPrenotazione, String date_from, String date_to, String name, String surname, String phone,
										 int cabins, int deckchairs, float price, String current_date_from, String current_date_to,
										 String IDCabins[] )
	{
		bookings.add( new Booking( IDPrenotazione, name, surname, phone, date_from, date_to, price, cabins, deckchairs, IDCabins ) );

		if(status == FREE && checkStatus( -1, current_date_from, current_date_to ) == OCCUPIED){
			button.setImageResource( R.drawable.ombrellone_rosso );
			status = OCCUPIED;
		}
	}

	/** modifica il periodo di prenotazione
	 * 
	 * @param IDPrenotazione - ID associato alla prenotazione da modificare
	 * @param date_from - inizio della nuova prenotazione
	 * @param date_to - scadenza della nuova prenotazione
	 * @param name - nome di chi ha effettuato la prenotazione
	 * @param surname - cognome di chi ha effettuato la prenotazione
	 * @param phone - numero di telefono di chi ha effettuato la prenotazione
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	 * @param price - il prezzo
	 * @param current_date_from - data di inizio periodo selezionata
	 * @param current_date_to - data di fine periodo selezionata
	 * @param IDCabins - lista degli ID delle cabine prenotate
	*/
	public synchronized void modifyBooking( int IDPrenotazione, String date_from, String date_to, String name, String surname, String phone,
											int cabins, int deckchairs, float price, String current_date_from, String current_date_to,
											String...IDCabins )
	{
		for(int i = bookings.size() - 1; i >= 0; i--){
			if(bookings.get( i ).getIDPrenotazione() == IDPrenotazione){
				bookings.get( i ).modifyBooking( date_from, date_to, name, surname, phone, cabins, deckchairs, price, IDCabins );

				if(checkStatus( -1, current_date_from, current_date_to ) == OCCUPIED){
					if(status == FREE){
						button.setImageResource( R.drawable.ombrellone_rosso );
						status = OCCUPIED;
					}
				}
				else{
					if(status == OCCUPIED){
						button.setImageResource( R.drawable.ombrellone_verde );
						status = FREE;
					}
				}

				break;
			}
		}
	}

	/** cancella la prenotazione
	 * 
	 * @param IDPrenotazione - ID associato alla prenotazione da cancellare
	 * @param date_from - data di inizio periodo
	 * @param date_to - data di fine periodo
	*/
	public synchronized void deleteBooking( int IDPrenotazione, String date_from, String date_to )
	{
		int size = bookings.size();

		for(int i = 0; i < size; i++){
			if(bookings.get( i ).getIDPrenotazione() == IDPrenotazione){
				bookings.remove( i ).deleteBooking();
				break;
			}
		}

		if(--size == 0 || checkStatus( -1, date_from, date_to ) == FREE){
			button.setImageResource( R.drawable.ombrellone_verde );
			status = FREE;
		}
	}
}