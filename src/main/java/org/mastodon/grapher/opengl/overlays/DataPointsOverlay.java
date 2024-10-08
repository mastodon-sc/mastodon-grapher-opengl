package org.mastodon.grapher.opengl.overlays;

import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataColor;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandlerOpenGL;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.scijava.listeners.Listeners;

public class DataPointsOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_NUM_DIMENSIONS = 2; // X, Y

	public static final int COLOR_NUM_CHANNELS = 4; // R, G, B, alpha

	public static final float DEFAULT_POINT_SIZE = 5.1f;

	private int vboVertexPositionHandle;

	private int vboVertexColorHandle;

	private float[] vertexPosData = new float[] {};

	private float[] vertexColorData = new float[] {};

	private boolean updateXY;

	private boolean updateColor;

	private final DataLayoutMaker layout;

	private final InertialScreenTransformEventHandlerOpenGL transformHandler;

	private final Listeners.List< LayoutChangeListener > layoutChangeListeners;

	public DataPointsOverlay( final DataLayoutMaker layout, final InertialScreenTransformEventHandlerOpenGL transformHandler )
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
		this.vboVertexPositionHandle = glGenBuffers();
		this.vboVertexColorHandle = glGenBuffers();
	}

	@Override
	public void paint()
	{
		glPointSize( DEFAULT_POINT_SIZE );

		if ( updateXY )
		{
			updateXY = false;

			// Update vertex XY.
			glBindBuffer( GL_ARRAY_BUFFER, vboVertexPositionHandle );
			glBufferData( GL_ARRAY_BUFFER, vertexPosData, GL_STATIC_DRAW );
			glVertexPointer( VERTEX_NUM_DIMENSIONS, GL_FLOAT, 0, 0 );
			glBindBuffer( GL_ARRAY_BUFFER, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			// Vertex colors.
			glBindBuffer( GL_ARRAY_BUFFER, vboVertexColorHandle );
			glBufferData( GL_ARRAY_BUFFER, vertexColorData, GL_STATIC_DRAW );
			glBindBuffer( GL_ARRAY_BUFFER, 0 );
		}

		/*
		 * Enable.
		 */

		glEnableClientState( GL_VERTEX_ARRAY );
		glEnableClientState( GL_COLOR_ARRAY );

		/*
		 * Draw vertices.
		 */

		// Vertex colors.
		glBindBuffer( GL_ARRAY_BUFFER, vboVertexPositionHandle );
		glVertexPointer( VERTEX_NUM_DIMENSIONS, GL_FLOAT, 0, 0 );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		glBindBuffer( GL_ARRAY_BUFFER, vboVertexColorHandle );
		glColorPointer( COLOR_NUM_CHANNELS, GL_FLOAT, 0, 0 );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		// Draw vertices as points.
		glDrawArrays( GL_POINTS, 0, vertexPosData.length / VERTEX_NUM_DIMENSIONS );

		/*
		 * Disable.
		 */

		glDisableClientState( GL_COLOR_ARRAY );
		glDisableClientState( GL_VERTEX_ARRAY );
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
