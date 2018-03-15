
package stefano.ceccotti.beachmanager;

import stefano.ceccotti.beachmanager.MainActivity.MyHandler;
import stefano.ceccotti.beachmanager.utils.Global;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefActivity extends PreferenceActivity
{
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		addPreferencesFromResource( R.xml.settings );
	}

	@Override
	protected void onDestroy()
	{
		Global.bm.updateSettings( this );
		MyHandler.updateSettings();

		super.onDestroy();
	}
}