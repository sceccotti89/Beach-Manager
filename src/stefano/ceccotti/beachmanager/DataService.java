
package stefano.ceccotti.beachmanager;

import stefano.ceccotti.beachmanager.engine.DataReceiver;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DataService extends Service
{
	@Override
	public IBinder onBind( Intent intent )
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		// TODO cambiare i parametri del thread nel caso venisse utilizzata questa classe
		Thread t = new Thread( new DataReceiver( null, null ) );
		t.setPriority( Thread.MAX_PRIORITY );
		t.start();
	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
}