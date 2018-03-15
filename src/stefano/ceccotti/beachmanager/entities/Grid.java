
package stefano.ceccotti.beachmanager.entities;

import stefano.ceccotti.beachmanager.utils.Global;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class Grid extends View
{
	/* area in cui inserire la griglia */
	private Paint grid;
	/* numero di colonne e righe della griglia */
	private int numRows, numColumns;
	/* area occupata dal testo */
	private Rect bound;
	/* determina se visualizzare la griglia */
	private boolean show_grid = true;
	/* determina se disegnare la griglia */
	//private boolean drawGrid = false;
	/* effetto tratteggiato */
	//private DashPathEffect dashed = new DashPathEffect( new float[]{ 5, 10, 15, 20 }, 0 );

	public Grid( Context context )
	{
		super( context );

		init();
	}

	public Grid( Context context, AttributeSet attrs )
	{
		super( context, attrs );

		init();
	}

	public Grid( Context context, AttributeSet attrs, int defStyle )
	{
		super( context, attrs, defStyle );

		init();
	}

	/** inizializza */
	private void init()
	{
		bound = new Rect();

		grid = new Paint();
		grid.setColor( Color.BLACK );
	}

	/** assegna le dimensioni della griglia
	 * @param rows - numero di righe
	 * @param columns - numero di colonne
	*/
	public void setBounds( int rows, int columns )
	{
		numRows = rows;
		numColumns = columns;

		//grid.setTextSize( 16 * (Global.ratioW / Global.ratioH) );
		grid.setTextSize( 16 );
		grid.setTextScaleX( Global.ratioW );

		requestLayout();
		invalidate();
	}

	/** aggiorna la griglia */
	public void update()
	{
		//grid.setTextSize( 16 * (Global.ratioW / Global.ratioH) );
		grid.setTextSize( 16 );
		grid.setTextScaleX( Global.ratioW );

		invalidate();
	}

	/** modifica la visibilita' della numerazione
	 * @param show - TRUE se visibile, FALSE altrimenti
	*/
	public void setShow( boolean show )
	{
		if(show != show_grid){
			show_grid = show;
			invalidate();
		}
	}

	/*
	/** determina se disegnare la griglia
	 * @param edit_mode - TRUE se e' in edit mode, FALSE altrimenti
	*/
	/*public void setEditMode( boolean edit_mode )
	{
		drawGrid = edit_mode;
	}*/

	@Override
	protected void onDraw( Canvas canvas )
	{
		if(show_grid && numRows > 0 && numColumns > 0){
			float height = getMeasuredHeight() - Global.offsetY;
			float h = height / numRows;
			float sizeW = Global.startX, startY = Global.offsetY + h / 2;

			for(int i = 0; i < numRows; i++){
				String value = (i + 1) + "";
				grid.getTextBounds( value, 0, value.length(), bound );
				canvas.drawText( value, sizeW / 2 - (bound.width() / 2), startY + i * h + (bound.height() / 2), grid );
			}

			float w = (getWidth() - Global.startX) / (numColumns);
			float startX = Global.startX + w / 2;
			startY = Global.offsetY / 2;

			for(int i = 0; i < numColumns; i++){
				String value = (i + 1) + "";
				grid.getTextBounds( value, 0, value.length(), bound );
				canvas.drawText( value, startX + i * w - (bound.width() / 2), startY + bound.height() / 2, grid );
			}

			// inserisce le righe e le colonne in caso di edit della spiaggia
			/*if(drawGrid){
				grid.setStrokeWidth( 2 );
				grid.setPathEffect( dashed );

				// TODO disegnare solo l'area della spiaggia selezionata durante l'edit
				

				float X = Global.startX, Y = Global.offsetY;
				float endX = Global.WIDTH, endY = Global.HEIGHT;

				for(int i = 0; i <= numColumns; i++){
					canvas.drawLine( X, Y, X, endY, grid );
					X = X + w;
				}

				X = Global.startX;
				for(int i = 0; i <= numRows; i++){
					canvas.drawLine( X, Y, endX, Y, grid );
					Y = Y + h;
				}

				grid.setPathEffect( null );
				grid.setStrokeWidth( 1 );
			}*/
		}

		super.onDraw( canvas );
	}
}