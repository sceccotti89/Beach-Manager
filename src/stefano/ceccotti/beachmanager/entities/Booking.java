
package stefano.ceccotti.beachmanager.entities;

import java.util.ArrayList;
import java.util.Collections;

import stefano.ceccotti.beachmanager.utils.Global;


public class Booking
{
	/* dati dell'utente */
	private String name, surname, phone;
	/* ID della prenotazione */
	private int IDPrenotazione;
	/* periodo della prenotazione */
	private String date_from = null, date_to = null;
	/* numero di sdraie prenotate */
	private int cabins, deckchairs;
	/* lista di cabine prenotate */
	private ArrayList<Integer> cabine;
	/* il prezzo */
	private float price;
	/* determina se la prenotazione e' attiva */
	private boolean isEnabled = true;

	public Booking( int IDPrenotazione, String name, String surname, String phone,
					String date_from, String date_to, float price, int cabins, int deckchairs, String IDCabins[] )
	{
		this.name = name;
		this.surname = surname;
		this.phone = phone;
		this.IDPrenotazione = IDPrenotazione;
		this.date_from = date_from;
		this.date_to = date_to;
		this.price = price;
		this.cabins = cabins;
		this.deckchairs = deckchairs;

		if(cabins > 0){
			cabine = new ArrayList<Integer>( cabins );
			// aggiunge gli ID delle cabine
			int length = IDCabins.length;
			for(int i = 0; i < cabins; i++)
				cabine.add( Integer.parseInt( IDCabins[length - i - 1] ) );

			Collections.sort( cabine );

			Global.free_cabins = Global.free_cabins - cabins;
		}

		Global.free_deckchairs = Global.free_deckchairs - deckchairs;
	}

	/** restituisce il nome dell'utente
	 * 
	 * @return il nome
	*/
	public String getName()
	{
		return name;
	}

	/** restituisce il cognome dell'utente
	 * 
	 * @return il cognome
	*/
	public String getSurname()
	{
		return surname;
	}

	/** restituisce il numero di telefono dell'utente
	 * 
	 * @return il numero
	*/
	public String getPhone()
	{
		return phone;
	}

	/** restituisce l'ID della prenotazione
	 * 
	 * @return l'identificativo
	*/
	public int getIDPrenotazione()
	{
		return IDPrenotazione;
	}

	/** restituisce la data di inizio prenotazione (formato gg-mm-aaaa)
	 * 
	 * @return la data di inizio
	*/
	public String getDateFrom()
	{
		return date_from;
	}

	/** restituisce la data di fine prenotazione (formato gg-mm-aaaa)
	 * 
	 * @return la data di fine
	*/
	public String getDateTo()
	{
		return date_to;
	}

	/** restituisce il numero di cabine prenotate
	 * 
	 * @return il numero di cabine
	*/
	public int getCabins()
	{
		return cabins;
	}

	/** restituisce l'ID della cabina alla posizione richiesta
	 * 
	 * @param index - indice della cabina richiesta
	 * 
	 * @return l'ID della cabina
	*/
	public int getCabinAtPosition( int index )
	{
		return cabine.get( index );
	}

	/** restituice il numero di sdraie prenotate
	 * 
	 * @return il numero di sdraie
	*/
	public int getDeckchairs()
	{
		return deckchairs;
	}

	/** modifica il prezzo della prenotazione
	 * 
	 * @param price - il nuovo prezzo
	*/
	public void setPrice( int price )
	{
		this.price = price;
	}

	/** restituisce il prezzo della prenotazione
	 * 
	 * @return il prezzo
	*/
	public float getPrice()
	{
		return price;
	}

	/** assegna un nuovo ID alla prenotazione
	 * 
	 * @param ID - ID della prenotazione
	*/
	public void setID( int ID )
	{
		IDPrenotazione = ID;
	}

	/** determina se la prenotazione e' abilitata
	 * 
	 * @return TRUE se la prenotazione e' abilitata, FALSE altrimenti
	*/
	public boolean isEnabled()
	{
		return isEnabled;
	}

	/** modifica lo stato della prenotazione
	 * 
	 * @return TRUE se la prenotazione e' abilitata, FALSE altrimenti
	*/
	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled;
	}

	/** modifica i dati della prenotazione
	 * 
	 * @param date_from - inizio della nuova prenotazione
	 * @param date_to - scadenza della nuova prenotazione
	 * @param name - nome di chi ha effettuato la prenotazione
	 * @param surname - cognome di chi ha effettuato la prenotazione
	 * @param phone - numero di telefono di chi ha effettuato la prenotazione
	 * @param cabins - numero di cabine prenotate
	 * @param deckchairs - numero di sdraie prenotate
	 * @param price - il prezzo
	 * @param IDCabins - lista di ID delle cabine prenotate
	*/
	public void modifyBooking( String date_from, String date_to, String name, String surname,
								String phone, int cabins, int deckchairs, float price, String IDCabins[] )
	{
		this.date_from = date_from;
		this.date_to = date_to;
		this.name = name;
		this.surname = surname;
		this.phone = phone;
		this.deckchairs = deckchairs;
		this.price = price;

		Global.free_cabins = Global.free_cabins - (cabins - this.cabins);
		Global.free_deckchairs = Global.free_deckchairs - (deckchairs - this.deckchairs);

		if(cabins < this.cabins){
			// toglie cabine (le prime)
			for(int i = this.cabins - cabins - 1; i >= 0; i--)
				cabine.remove( i );

			this.cabins = cabins;
		}
		else{
			if(cabins > this.cabins){
				if(cabine == null)
					cabine = new ArrayList<Integer>( cabins - this.cabins );

				// aggiunge cabine
				int length = IDCabins.length;
				for(int i = cabins - this.cabins - 1; i >= 0; i--)
					cabine.add( Integer.parseInt( IDCabins[length - i - 1] ) );

				Collections.sort( cabine );

				this.cabins = cabins;
			}
		}
	}

	/** rimuove la prenotazione */
	public void deleteBooking()
	{
		cabine.clear();

		Global.free_cabins = Global.free_cabins + cabins;
		Global.free_deckchairs = Global.free_deckchairs + deckchairs;
	}
}