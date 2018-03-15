
package stefano.ceccotti.beachmanager;

import stefano.ceccotti.beachmanager.utils.Global;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

public class HelpActivity extends Activity
{
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if(Global.edit_mode)
			setContentView( R.layout.help_edit_activity );
		else
			setContentView( R.layout.help_main_activity );
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged( newConfig );

		Global.init( null, true );

		Global.bm.updatePadding();
	}
}