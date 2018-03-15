
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

public class BookingAdapter extends BaseAdapter
{
	/* lista di prenotazioni */
	private ArrayList<Booking> bookings;
	/* l'activity associata */
	private Activity activity;

	public BookingAdapter( Activity activity, ArrayList<Booking> objects )
	{
		bookings = objects;
		this.activity = activity;
    }

	/** assegna la lista di prenotazioni
	 * 
	 * @param bookings - lista di prenotazioni
	*/
	public void setBookingsList( ArrayList<Booking> bookings )
	{
		this.bookings = bookings;
	}

	@Override
	public int getCount()
	{
		return bookings.size();
	}

	@Override
	public Booking getItem( int position )
	{
		return bookings.get( position );
	}

	@Override
	public long getItemId( int position )
	{
		return position;
	}

	/** modifica la visibilita' di un oggetto
	 * 
	 * @param position - la posizione
	 * @param isEnabled - TRUE se visibile, FALSE altrimenti
	*/
	public void setEnabled( int position, boolean isEnabled )
	{
		bookings.get( position ).setEnabled( isEnabled );
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		ViewHolder viewHolder = null;

		if(convertView == null){
			LayoutInflater li = activity.getLayoutInflater();
	        convertView = li.inflate( R.layout.booking_elem_layout, (ViewGroup) activity.findViewById( R.id.booking_elem_layout_root ) );

			viewHolder = new ViewHolder();
			viewHolder.number = (TextView) convertView.findViewById( R.id.textViewNumber );
            viewHolder.from = (TextView) convertView.findViewById( R.id.textViewFrom );
            viewHolder.to = (TextView) convertView.findViewById( R.id.textViewTo );
            viewHolder.name = (TextView) convertView.findViewById( R.id.textViewName );
            viewHolder.surname = (TextView) convertView.findViewById( R.id.textViewSurname );
            viewHolder.phone = (TextView) convertView.findViewById( R.id.textViewPhone );
            viewHolder.cabins = (TextView) convertView.findViewById( R.id.textViewCabins );
            viewHolder.deckchairs = (TextView) convertView.findViewById( R.id.textViewDeckchairs );
            viewHolder.price = (TextView) convertView.findViewById( R.id.textViewPrice );

            convertView.setTag( viewHolder );
		}
		else
			viewHolder = (ViewHolder) convertView.getTag();

		Booking bkt = bookings.get( position );

		viewHolder.number.setText( activity.getString( R.string.booking ) + " " + (position + 1) );
		viewHolder.from.setText( activity.getString( R.string.from ) + " " + bkt.getDateFrom() );
		viewHolder.to.setText( activity.getString( R.string.to ) + " " + bkt.getDateTo() );
		//if(!bkt.getName().equals( "-" ))
			viewHolder.name.setText( activity.getString( R.string.name ) + ": " + bkt.getName() );
		//else
			//viewHolder.name.setVisibility( EditText.GONE );
		//if(!bkt.getSurname().equals( "-" ))
			viewHolder.surname.setText( activity.getString( R.string.surname ) + ": " + bkt.getSurname() );
		//else
			//viewHolder.surname.setVisibility( EditText.GONE );
		//if(!bkt.getPhone().equals( "-" ))
			viewHolder.phone.setText( activity.getString( R.string.phone ) + ": " + bkt.getPhone() );
		//else
			//viewHolder.phone.setVisibility( EditText.GONE );
		viewHolder.price.setText( activity.getString( R.string.price ) + ": " + bkt.getPrice() );

		int cabins = bkt.getCabins();
		if(cabins == 0)
			viewHolder.cabins.setText( activity.getString( R.string.cabin ) + ": -" );
		else{
			// inserisce il numero di ciascuna cabina prenotata
			String text = "";
			for(int i = 0; i < cabins - 1; i++)
				text = text + "n° " + bkt.getCabinAtPosition( i ) + " - ";
			text = text + "n° " + bkt.getCabinAtPosition( cabins - 1 );

			viewHolder.cabins.setText( activity.getString( R.string.cabin ) + ": " + text );
		}

		//viewHolder.cabins.setText( activity.getString( R.string.cabin ) + ": " + bkt.getCabins() );

		viewHolder.deckchairs.setText( activity.getString( R.string.deckchair ) + ": " + bkt.getDeckchairs() );

		convertView.setId( position );

		if(!bkt.isEnabled())
			((LinearLayout) convertView.findViewById( R.id.booking_elem )).setVisibility( View.GONE );
		else
			((LinearLayout) convertView.findViewById( R.id.booking_elem )).setVisibility( View.VISIBLE );

		return convertView;
	}

	/**
	 * Classe privata per la gestione della view delle prenotazioni
	*/
	private class ViewHolder
	{
		private TextView number;
		private TextView from;
		private TextView to;
		private TextView name;
		private TextView surname;
		private TextView phone;
		private TextView cabins;
		private TextView deckchairs;
		private TextView price;
	}
}