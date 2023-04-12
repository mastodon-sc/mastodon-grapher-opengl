package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayout;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandler;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.scijava.listeners.Listeners;

public class DataPointsOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	private int vboVertexHandle;

	private int vboColorHandle;

	private int nPoints = 0;

	private final float pointSize = 5.1f;

	private float[] xyData;

	private float[] colorData;

	private boolean updateXY;

	private boolean updateColor;

	private final DataLayout layout;

	private final InertialScreenTransformEventHandler transformHandler;

	private final Listeners.List< LayoutChangeListener > layoutChangeListeners;


	public DataPointsOverlay( final DataLayout layout, final InertialScreenTransformEventHandler transformHandler )
	{
		this.layout = layout;
		this.transformHandler = transformHandler;
		this.layoutChangeListeners = new Listeners.SynchronizedList<>();
	}

	public Listeners.List< LayoutChangeListener > getLayoutChangeListeners()
	{
		return layoutChangeListeners;
	}

	private void putCoords( final float[] xyData )
	{
		this.xyData = xyData;
		this.updateXY = true;
		// Update min & max.
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for ( int i = 0; i < xyData.length; i++ )
		{
			final float x = xyData[ i ];
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );

			i++;
			final float y = xyData[ i ];
			minY = Math.min( minY, y );
			maxY = Math.max( maxY, y );
		}
		final float layoutMinX = minX;
		final float layoutMinY = minY;
		final float layoutMaxX = maxX;
		final float layoutMaxY = maxY;
		layoutChangeListeners.list.forEach( l -> l.layoutChanged( layoutMinX, layoutMaxX, layoutMinY, layoutMaxY ) );
	}

	private void putColors( final float[] colorData )
	{
		this.colorData = colorData;
		this.updateColor = true;
	}

	@Override
	public void init()
	{
		// New handles.
		this.vboVertexHandle = GL33.glGenBuffers();
		this.vboColorHandle = GL33.glGenBuffers();
	}

	@Override
	public void paint()
	{
		GL33.glPointSize( pointSize );

		if ( updateXY )
		{
			nPoints = xyData.length / 2;
			updateXY = false;

			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, xyData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( VERTEX_SIZE, GL33.GL_FLOAT, 0, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboColorHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, colorData, GL33.GL_STATIC_DRAW );
			GL33.glColorPointer( COLOR_SIZE, GL33.GL_FLOAT, 0, 0 );
		}

		GL33.glEnableClientState( GL33.GL_VERTEX_ARRAY );
		GL33.glEnableClientState( GL33.GL_COLOR_ARRAY );

		// Draw vertices.
		GL33.glDrawArrays( GL33.GL_POINTS, 0, nPoints );
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

		GL33.glDisableClientState( GL33.GL_COLOR_ARRAY );
		GL33.glDisableClientState( GL33.GL_VERTEX_ARRAY );
	}

	public void plot( final FeatureGraphConfig graphConfig )
	{
		final float[] xy = layout.layout();
		final float[] color = layout.color();
		putCoords( xy );
		putColors( color );
		transformHandler.layoutChanged( xy );
	}
	
	public void updateColors()
	{
		final float[] color = layout.color();
		putColors( color );
	}
}
