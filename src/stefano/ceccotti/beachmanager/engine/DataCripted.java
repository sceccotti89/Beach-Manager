
package stefano.ceccotti.beachmanager.engine;

public class DataCripted
{
	/* valori utilizzati nell'RSA */
	private static final int E = 577, P = 131, Q = 359, N = P * Q;
	/* dimensione massima di un blocco */
	private static final int BLOCK = (int)(Math.log( N ) / Math.log( 2 )) + 2;
	/* alfabeto utilizzato per criptare un messaggio */
	private static final String alphabeth = "abcdefghijklmnopqrstuvxywzABCDEFGHIJKLMNOPQRSTUVXYWZ0123456789!\"£$%&/()=?^,;.:-_";
	/* lunghezza dell'alfabeto */
	private static final int length = alphabeth.length();
	/* chiave utilizzata per criptare il messaggio attraverso lo XOR */
	private static final String KEY = "1001100101100110110101101001010110100101010110110101010110101010" +
									  "0101001001010101010101010101110101010100010110001100100101000101" +
									  "0101001010001010101101000010101010101100101001010000001010101011" +
									  "1111001111010101101101010100011010101101010101010010110010011000";
	/* lunghezza della chiave */
	private static final int k_length = KEY.length();

	/** ottiene l'indice corrispondente al carattere nell'alfabeto
	 * @param value - il valore da ricercare
	 * 
	 * @return l'indice nel vettore se l'ha trovato, -1 altrimenti
	*/
	private static int searchIndex( char value )
	{
		for(int i = 0; i < length; i++)
			if(alphabeth.charAt( i ) == value)
				return i;

		return -1;
	}

	/** restituisce il valore binario del valore in input
	 * @param value - il valore da convertire
	 * 
	 * @return la stringa binaria
	*/
	private static String getBinary( long value )
	{
		String binary = "";

		if(value > 0){
			while(value != 0){
				// se value e' pari aggiunge (in cima) 0, altrimenti 1
				if(value % 2 == 0)
					binary = "0" + binary;
				else
					binary = "1" + binary;
				value = value / 2;
			}
		}
		else
			binary = "0";

		// se il numero generato e' piu' piccolo di un blocco vengono aggiunti degli 0 per completare
		if(binary.length() < BLOCK){
			for(int i = binary.length(); i < BLOCK; i++)
				binary = "0" + binary;
		}

		return binary;
	}

	/** calcola l'RSA sul valore in input
	 * @param value - il valore da criptare
	 * 
	 * @return l'input criptato
	*/
	private static long RSA( int value )
	{
		long key;

		key = 1;
		for(int j = 0; j < E; j++)
			key = key * value % N;

		return key;
	}

	/** restituisce il messaggio criptato
	 * @param message - il messaggio da criptare
	 *
	 * @return il messaggio criptato
	*/
	public static String encrypt( String message )
	{
		return encrypt_msg( message );
	}

	/** cripta il messaggio
	 * @param message - il messaggio da criptare
	 * 
	 * @return il messaggio criptato
	*/
	private static String encrypt_msg( String message )
	{
		int i, j;
		long key;
		String temp, buffer = "", encrypted = "";

		// calcola il numero casuale per lo XOR
		int index = (int)(Math.random() * k_length);

		// aggiunge in testa il valore casuale utilizzato per lo XOR
		key = RSA( index );
		temp = getBinary( key );

		for(i = 0; i < BLOCK; i++){
			if(temp.charAt( i ) == KEY.charAt( i ))
				buffer = buffer + "0";
			else
				buffer = buffer + "1";
		}

		int length = message.length();

		// inserisce la lunghezza originale del messaggio
		temp = getBinary( RSA( length ) );

		// cripta il messaggio
		for(i = 0; i < length; i++){
			key = RSA( searchIndex( message.charAt( i ) ) );

			// ogni carattere criptato viene trasformato in binario
			temp = temp + getBinary( key );
		}

		// esegue lo XOR tra i bit del messaggio e quelli della chiave
		int dim = temp.length();
		for(i = 0; i < dim; i++){
			if(temp.charAt( i ) == KEY.charAt( index ))
				buffer = buffer + "0";
			else
				buffer = buffer + "1";

			index = (index + 1) % k_length;
		}

		// se il messaggio originale e' piu' corto della lunghezza massima del testo, aggiungiamo blocchi con valori casuali (PADDING)
		for(i = length; i < 15; i++){
			for(j = 0; j < BLOCK; j++)
				buffer = buffer + (int)(2 * Math.random());
		}

		// inverte il messaggio
		length = buffer.length();
		for(i = length  - 1; i >= 0; i--)
			encrypted = encrypted + buffer.charAt( i );

		return encrypted;
	}
}