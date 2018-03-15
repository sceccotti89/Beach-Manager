
package stefano.ceccotti.beachmanager.entities;


public class Rate
{
	/* ID della tariffa */
	private int IDRate;
	/* data di inizio e di fine periodo */
	private String date_from, date_to;
	/* prezzo giornaliero e settimanale */
	private float day_price, week_price;
	/* determina se la tariffa e' attiva */
	private boolean isEnabled = true;

	/* stato della tariffa */
	public static final int FREE = 0, OCCUPIED = 1;

	public Rate( int IDRate, String date_from, String date_to, float day_price, float week_price )
	{
		this.IDRate = IDRate;
		this.date_from = date_from;
		this.date_to = date_to;
		this.day_price = day_price;
		this.week_price = week_price;
	}

	/** restituisce l'ID della tariffa
	 * @return l'ID
	*/
	public synchronized int getIDRate()
	{
		return IDRate;
	}

	/** assegna un nuovo ID alla prenotazione
	 * @param ID - il nuovo ID della prenotazione
	*/
	public synchronized void setID( int ID )
	{
		IDRate = ID;
	}

	/** restituisce la data di inizio periodo
	 * @return la data di inizio
	*/
	public synchronized String getDateFrom()
	{
		return date_from;
	}

	/** restituisce la data di fine periodo
	 * @return la data di fine
	*/
	public synchronized String getDateTo()
	{
		return date_to;
	}

	/** restituisce il prezzo giornaliero
	 * @return il prezzo
	*/
	public synchronized float getDailyPrice()
	{
		return day_price;
	}

	/** restituisce il prezzo settimanale
	 * @return il prezzo
	*/
	public synchronized float getWeeklyPrice()
	{
		return week_price;
	}

	/** determina se la tariffa e' abilitata
	 * @return TRUE se la tariffa e' abilitata, FALSE altrimenti
	*/
	public boolean isEnabled()
	{
		return isEnabled;
	}

	/** modifica lo stato della tariffa
	 * @return TRUE se la tariffa e' abilitata, FALSE altrimenti
	*/
	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled;
	}

	/** modifica il pacchetto
	 * @param date_from - data di inizio periodo
	 * @param date_to - data di fine periodo
	 * @param day_price - prezzo giornaliero
	 * @param week_price - prezzo mensile
	*/
	public synchronized void modifyRate( String date_from, String date_to, float day_price, float week_price )
	{
		this.date_from = date_from;
		this.date_to = date_to;
		this.day_price = day_price;
		this.week_price = week_price;
	}
}