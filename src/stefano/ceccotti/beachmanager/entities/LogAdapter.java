
package stefano.ceccotti.beachmanager.entities;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.R;
import stefano.ceccotti.beachmanager.utils.Log;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LogAdapter extends BaseAdapter
{
	/* lista di pacchetti */
	private ArrayList<Log> logs;
	/* l'activity associata */
	private Activity activity;

	public LogAdapter( Activity activity, ArrayList<Log> objects )
	{
		logs = objects;
		this.activity = activity;
    }

	/** assegna la lista dei log
	 * 
	 * @param objects - lista dei log
	*/
	public void setLogList( ArrayList<Log> objects )
	{
		logs = objects;
	}

	@Override
	public int getCount()
	{
		return logs.size();
	}

	@Override
	public Log getItem( int position )
	{
		return logs.get( position );
	}

	@Override
	public long getItemId( int position )
	{
		return position;
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		ViewHolder viewHolder = null;

		if(convertView == null){
			LayoutInflater inflater = LayoutInflater.from( activity );
			convertView = inflater.inflate( R.layout.log_elem_layout, (ViewGroup) activity.findViewById( R.id.log_elem_layout_root ) );

			viewHolder = new ViewHolder();
			viewHolder.number = (TextView) convertView.findViewById( R.id.textViewNumber );
			viewHolder.text = (TextView) convertView.findViewById( R.id.textViewText );
            viewHolder.date = (TextView) convertView.findViewById( R.id.textViewDate );

            convertView.setTag( viewHolder );
		}
		else
			viewHolder = (ViewHolder) convertView.getTag();

		Log log = logs.get( position );

		viewHolder.number.setText( activity.getString( R.string.log ) + " " + (position + 1) );
		viewHolder.text.setText( activity.getString( R.string.text ) + " " + log.getText() );
		viewHolder.date.setText( activity.getString( R.string.date ) + " " + log.getDate() );

		convertView.setId( position );

		return convertView;
	}

	/**
	 * Classe privata per la gestione della view dei log
	*/
	private class ViewHolder
	{
		private TextView number;
		private TextView text;
		private TextView date;
	}
}