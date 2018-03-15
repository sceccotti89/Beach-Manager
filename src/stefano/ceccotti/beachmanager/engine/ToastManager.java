
package stefano.ceccotti.beachmanager.engine;

import stefano.ceccotti.beachmanager.R;
import stefano.ceccotti.beachmanager.utils.Global;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ToastManager
{
	public ToastManager()
	{
		
	}

	/** crea un nuovo toast
	 * 
	 * @param activity - l'activity associata
	 * @param textID - ID del testo da mostrare
	*/
	public void addToast( Activity activity, int textID )
	{
		LayoutInflater inflater = activity.getLayoutInflater();
		View v = inflater.inflate( R.layout.toast_layout, (ViewGroup) activity.findViewById( R.id.toast_layout_root ) );

		TextView text = (TextView) v.findViewById( R.id.text );
		text.setText( textID );
		text.setLayoutParams( new LinearLayout.LayoutParams( (int)(Global.sizewBox * 2.5f), LayoutParams.WRAP_CONTENT ) );

		Toast toast = new Toast( activity );
		toast.setView( v );
		toast.setDuration( Toast.LENGTH_LONG );
	}
}