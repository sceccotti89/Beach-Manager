
package stefano.ceccotti.beachmanager.entities;

import java.util.ArrayList;

import stefano.ceccotti.beachmanager.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RateAdapter extends BaseAdapter
{
	/* lista di pacchetti */
	private ArrayList<Rate> packets;
	/* l'activity associata */
	private Activity activity;

	public RateAdapter( Activity activity, ArrayList<Rate> objects )
	{
		packets = objects;
		this.activity = activity;
    }

	/** assegna la lista delle tariffe
	 * 
	 * @param rates - lista delle tariffe
	*/
	public void setRateList( ArrayList<Rate> rates )
	{
		packets = rates;
	}

	@Override
	public int getCount()
	{
		return packets.size();
	}

	@Override
	public Rate getItem( int position )
	{
		return packets.get( position );
	}

	@Override
	public long getItemId( int position )
	{
		return position;
	}

	/** modifica la visibilita' di un oggetto
	 * @param position - la posizione
	 * @param isEnabled - TRUE se visibile, FALSE altrimenti
	*/
	public void setEnabled( int position, boolean isEnabled )
	{
		packets.get( position ).setEnabled( isEnabled );
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		ViewHolder viewHolder = null;

		if(convertView == null){
			LayoutInflater inflater = LayoutInflater.from( activity );
			convertView = inflater.inflate( R.layout.rate_elem_layout, (ViewGroup) activity.findViewById( R.id.packet_elem_layout_root ) );

			viewHolder = new ViewHolder();
			viewHolder.number = (TextView) convertView.findViewById( R.id.textViewNumber );
            viewHolder.from = (TextView) convertView.findViewById( R.id.textViewFrom );
            viewHolder.to = (TextView) convertView.findViewById( R.id.textViewTo );
            viewHolder.day_price = (TextView) convertView.findViewById( R.id.textViewDayPrice );
            viewHolder.week_price = (TextView) convertView.findViewById( R.id.textViewWeekPrice );

            convertView.setTag( viewHolder );
		}
		else
			viewHolder = (ViewHolder) convertView.getTag();

		Rate rate = packets.get( position );

		viewHolder.number.setText( activity.getString( R.string.tariff ) + " " + (position + 1) );
		viewHolder.from.setText( activity.getString( R.string.from ) + " " + rate.getDateFrom() );
		viewHolder.to.setText( activity.getString( R.string.to ) + " " + rate.getDateTo() );
		viewHolder.day_price.setText( activity.getString( R.string.daily ) + " " + rate.getDailyPrice() );
		viewHolder.week_price.setText( activity.getString( R.string.weekly ) + " " + rate.getWeeklyPrice() );

		convertView.setId( position );

		if(!rate.isEnabled())
			((LinearLayout) convertView.findViewById( R.id.rate_elem )).setVisibility( View.GONE );
		else
			((LinearLayout) convertView.findViewById( R.id.rate_elem )).setVisibility( View.VISIBLE );

		return convertView;
	}

	/**
	 * Classe privata per la gestione della view delle tariffe
	*/
	private class ViewHolder
	{
		private TextView number;
		private TextView from;
		private TextView to;
		private TextView day_price;
		private TextView week_price;
	}
}