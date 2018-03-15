
package stefano.ceccotti.beachmanager.entities;

public class Cabin
{
	/* ID della cabina */
	private int IDCabin;

	public Cabin( int IDCabin )
	{
		this.IDCabin = IDCabin;
	}

	/** modifica l'ID della cabina
	 * @param IDCabin - il nuovo ID della cabina
	*/
	public void setIDCabin( int IDCabin )
	{
		this.IDCabin = IDCabin;
	}

	/** restituisce l'ID della cabina
	 * @return l'ID della cabina
	*/
	public int getIDCabin()
	{
		return IDCabin;
	}
}