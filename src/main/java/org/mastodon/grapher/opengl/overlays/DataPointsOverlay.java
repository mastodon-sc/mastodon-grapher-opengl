package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandler;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.scijava.listeners.Listeners;

public class DataPointsOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	private static final int EDGE_SIZE = 2; // source ID, target ID

	private int vboVertexHandle;

	private int vboColorHandle;

	private int iboEdgeIndexHandle;

	private final float pointSize = 5.1f;

	private float[] vertexPosData = new float[] {};

	private float[] vertexColorData = new float[] {};

	private int[] edgeIndexData = new int[] {};

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

	private void putCoords( final float[] xyData, final int[] indices )
	{
		this.vertexPosData = xyData;
		this.edgeIndexData = indices;
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
		this.vertexColorData = colorData;
		this.updateColor = true;
	}

	@Override
	public void init()
	{
		// New handles.
		this.vboVertexHandle = GL33.glGenBuffers();
		this.vboColorHandle = GL33.glGenBuffers();
		this.iboEdgeIndexHandle = GL33.glGenBuffers();
	}

	@Override
	public void paint()
	{
		GL33.glPointSize( pointSize );

		if ( updateXY )
		{
			updateXY = false;

			// Update vertex XY.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboVertexHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, vertexPosData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( VERTEX_SIZE, GL33.GL_FLOAT, 0, 0 );

			// Update edges.
			GL33.glBindBuffer( GL33.GL_ELEMENT_ARRAY_BUFFER, iboEdgeIndexHandle );
			GL33.glBufferData( GL33.GL_ELEMENT_ARRAY_BUFFER, edgeIndexData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( EDGE_SIZE, GL33.GL_UNSIGNED_INT, 0, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			// Update vertex colors.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboColorHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, vertexColorData, GL33.GL_STATIC_DRAW );
			GL33.glColorPointer( COLOR_SIZE, GL33.GL_FLOAT, 0, 0 );
		}

		/*
		 * Enable.
		 */

		GL33.glEnableClientState( GL33.GL_VERTEX_ARRAY );
		GL33.glEnableClientState( GL33.GL_COLOR_ARRAY );

		/*
		 * Draw edges.
		 */

		// Enable and set the position attribute.
		GL33.glEnableVertexAttribArray( 0 );
		GL33.glVertexAttribPointer( 0, VERTEX_SIZE, GL33.GL_FLOAT, false, 0, 0 );

		// Draw the line segments using the indices.
		GL33.glDrawElements( GL33.GL_LINES, edgeIndexData.length, GL33.GL_UNSIGNED_INT, 0 );

		// Disable the position attribute.
		GL33.glDisableVertexAttribArray( 0 );

		/*
		 * Draw vertices.
		 */

		GL33.glDrawArrays( GL33.GL_POINTS, 0, vertexPosData.length / VERTEX_SIZE );
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

		/*
		 * Disable.
		 */

		GL33.glDisableClientState( GL33.GL_COLOR_ARRAY );
		GL33.glDisableClientState( GL33.GL_VERTEX_ARRAY );
	}

	public void plot( final FeatureGraphConfig graphConfig )
	{
		final DataLayout l = layout.layout();
		final float[] xy = l.verticesPos;
		final int[] indices = l.edgeIndices;
		final float[] color = layout.color();
		putCoords( xy, indices );
		putColors( color );
		transformHandler.layoutChanged( xy );
	}


	public void updateColors()
	{
		final float[] color = layout.color();
		putColors( color );
	}
}
