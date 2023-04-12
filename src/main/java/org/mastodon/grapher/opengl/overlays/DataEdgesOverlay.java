package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataColor;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.scijava.listeners.Listeners;

public class DataEdgesOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	private static final int EDGE_SIZE = 2; // source ID, target ID

	private int iboEdgeIndexHandle;

	private int vboEdgePositionHandle;

	private int vboEdgeColorHandle;

	private int[] edgeIndexData = new int[] {};

	private float[] edgePosData = new float[] {};

	private float[] edgesColorData = new float[] {};

	private boolean updateXY;

	private boolean updateColor;

	private final DataLayoutMaker layout;

	private final Listeners.List< LayoutChangeListener > layoutChangeListeners;

	public DataEdgesOverlay( final DataLayoutMaker layout )
	{
		this.layout = layout;
		this.layoutChangeListeners = new Listeners.SynchronizedList<>();
	}

	public Listeners.List< LayoutChangeListener > getLayoutChangeListeners()
	{
		return layoutChangeListeners;
	}

	private void putCoords( final int[] indices, final float[] edgePosData )
	{
		this.edgeIndexData = indices;
		this.edgePosData = edgePosData;
		this.updateXY = true;
	}

	private void putColors( final float[] edgesColor )
	{
		this.edgesColorData = edgesColor;
		this.updateColor = true;
	}

	@Override
	public void init()
	{
		// New handles.
		this.vboEdgePositionHandle = GL33.glGenBuffers();
		this.iboEdgeIndexHandle = GL33.glGenBuffers();
		this.vboEdgeColorHandle = GL33.glGenBuffers();
	}

	@Override
	public void paint()
	{
		if ( updateXY )
		{
			updateXY = false;

			// Update edge indices.
			GL33.glBindBuffer( GL33.GL_ELEMENT_ARRAY_BUFFER, iboEdgeIndexHandle );
			GL33.glBufferData( GL33.GL_ELEMENT_ARRAY_BUFFER, edgeIndexData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( EDGE_SIZE, GL33.GL_UNSIGNED_INT, 0, 0 );
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

			// Update edge position.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboEdgePositionHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, edgePosData, GL33.GL_STATIC_DRAW );
			GL33.glVertexPointer( VERTEX_SIZE, GL33.GL_FLOAT, 0, 0 );
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			// Update edge colors.
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboEdgeColorHandle );
			GL33.glBufferData( GL33.GL_ARRAY_BUFFER, edgesColorData, GL33.GL_STATIC_DRAW );
			GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );
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
		GL33.glEnableVertexAttribArray( 1 );
		GL33.glVertexAttribPointer( 1, VERTEX_SIZE, GL33.GL_FLOAT, false, 0, 0 );

		// Edge colors.
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, vboEdgeColorHandle );
		GL33.glColorPointer( COLOR_SIZE, GL33.GL_FLOAT, 0, 0 );

		// Draw the line segments using the indices.
		GL33.glDrawElements( GL33.GL_LINES, edgeIndexData.length, GL33.GL_UNSIGNED_INT, 0 );
		GL33.glBindBuffer( GL33.GL_ARRAY_BUFFER, 0 );

		// Disable the position attribute.
		GL33.glDisableVertexAttribArray( 1 );

		/*
		 * Disable.
		 */

		GL33.glDisableClientState( GL33.GL_COLOR_ARRAY );
		GL33.glDisableClientState( GL33.GL_VERTEX_ARRAY );
	}

	public void plot( final FeatureGraphConfig graphConfig )
	{
		final DataLayout l = layout.layout();
		putCoords( l.edgeIndices, l.edgePositions );
		final DataColor c = layout.color();
		putColors( c.edgesColor );
	}

	public void updateColors()
	{
		final DataColor c = layout.color();
		putColors( c.edgesColor );
	}
}
