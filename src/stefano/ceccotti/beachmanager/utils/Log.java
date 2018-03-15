
package stefano.ceccotti.beachmanager.utils;

public class Log
{
	/* il testo */
	private String text;
	/* la data */
	private String date;
	/* l'ID del log */
	private int ID;
	/* il prossimo ID da assegnare */
	private static int nextID = 0;

	public Log( String text, String date )
	{
		this.text = text;
		this.date = date;
		ID = nextID;
		nextID++;
	}

	/** restituisce il testo del log
	 * 
	 * @return il testo
	*/
	public String getText()
	{
		return text;
	}

	/** restituisce la data del log
	 * 
	 * @return la data
	*/
	public String getDate()
	{
		return date;
	}

	/** restituisce l'ID del log
	 * 
	 * @return l'ID
	*/
	public int getID()
	{
		return ID;
	}
}