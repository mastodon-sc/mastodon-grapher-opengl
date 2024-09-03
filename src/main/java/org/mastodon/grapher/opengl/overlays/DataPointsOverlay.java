package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataColor;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandler;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.scijava.listeners.Listeners;

public class DataPointsOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	public static final float DEFAULT_POINT_SIZE = 5.1f;

	private int vboVertexPositionHandle;

	private int vboVertexColorHandle;

	private float[] vertexPosData = new float[] {};

	private float[] vertexColorData = new float[] {};

	private boolean updateXY;

	private boolean updateColor;

	private final DataLayoutMaker layout;

	private final InertialScreenTransformEventHandler transformHandler;

	private final Listeners.List< LayoutChangeListener > layoutChangeListeners;

	public DataPointsOverlay( final DataLayoutMaker layout, final InertialScreenTransformEventHandler transformHandler )
	{
		this.layout = layout;
		this.transformHandler = transformHandler;
		this.layoutChangeListeners = new Listeners.SynchronizedList<>();
	}

	public Listeners.List< LayoutChangeListener > getLayoutChangeListeners()
	{
		return layoutChangeListeners;
	}

	private void putCoords( final float[] vertexPosData )
	{
		this.vertexPosData = vertexPosData;
		this.updateXY = true;
		// Update min & max.
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for ( int i = 0; i < vertexPosData.length; i++ )
		{
			final float x = vertexPosData[ i ];
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );

			i++;
			final float y = vertexPosData[ i ];
			minY = Math.min( minY, y );
			maxY = Math.max( maxY, y );
		}
		final float layoutMinX = minX;
		final float layoutMinY = minY;
		final float layoutMaxX = maxX;
		final float layoutMaxY = maxY;
		layoutChangeListeners.list.forEach( l -> l.layoutChanged( layoutMinX, layoutMaxX, layoutMinY, layoutMaxY ) );
	}

	private void putColors( final float[] verticesColor )
	{
		this.vertexColorData = verticesColor;
		this.updateColor = true;
	}

	@Override
	public void init()
	{
		// New handles.
		this.vboVertexPositionHandle = GL33.glGenBuffers();
		this.vboVertexColorHandle = GL33.glGenBuffers();
	}

	@Override
	public void paint()
	{
		GL33.glPointSize( DEFAULT_POINT_SIZE );

		if ( updateXY )
		{
			updateXY = false;

			// Update vertex XY.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexPositionHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, vertexPosData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( VERTEX_SIZE, GL33.GL_FLOAT, 0, 0 );
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			// Vertex colors.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexColorHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, vertexColorData, GL33.GL_STATIC_DRAW );
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );
		}

		/*
		 * Enable.
		 */

		GL33.glEnableClientState( GL33.GL_VERTEX_ARRAY );
		GL33.glEnableClientState( GL33.GL_COLOR_ARRAY );

		/*
		 * Draw vertices.
		 */

		// Vertex colors.
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexPositionHandle );
		GL33.glVertexPointer( VERTEX_SIZE, GL33.GL_FLOAT, 0, 0 );
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexColorHandle );
		GL33.glColorPointer( COLOR_SIZE, GL33.GL_FLOAT, 0, 0 );
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

		// Draw vertices as points.
		GL33.glDrawArrays( GL33.GL_POINTS, 0, vertexPosData.length / VERTEX_SIZE );

		/*
		 * Disable.
		 */

		GL33.glDisableClientState( GL33.GL_COLOR_ARRAY );
		GL33.glDisableClientState( GL33.GL_VERTEX_ARRAY );
	}

	public void draw( final DataLayout l )
	{
		putCoords( l.verticesPos );
		final DataColor c = layout.color();
		putColors( c.verticesColor );
		transformHandler.layoutChanged( l.verticesPos );
	}

	public void updateColors()
	{
		final DataColor c = layout.color();
		putColors( c.verticesColor );
	}
}
