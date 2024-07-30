package org.mastodon.grapher.opengl.overlays;

import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataColor;
import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.scijava.listeners.Listeners;

public class DataEdgesOverlay implements GLOverlayRenderer
{

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

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
		this.vboEdgePositionHandle = glGenBuffers();
		this.iboEdgeIndexHandle = glGenBuffers();
		this.vboEdgeColorHandle = glGenBuffers();
	}

	@Override
	public void paint()
	{
		if ( updateXY )
		{
			updateXY = false;

			// Update edge indices.
			glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, iboEdgeIndexHandle );
			glBufferData( GL_ELEMENT_ARRAY_BUFFER, edgeIndexData, GL_STATIC_DRAW );
			glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, 0 );

			// Update edge position.
			glBindBuffer( GL_ARRAY_BUFFER, vboEdgePositionHandle );
			glBufferData( GL_ARRAY_BUFFER, edgePosData, GL_STATIC_DRAW );
			glBindBuffer( GL_ARRAY_BUFFER, 0 );
		}
		if ( updateColor )
		{
			updateColor = false;

			// Update edge colors.
			glBindBuffer( GL_ARRAY_BUFFER, vboEdgeColorHandle );
			glBufferData( GL_ARRAY_BUFFER, edgesColorData, GL_STATIC_DRAW );
			glBindBuffer( GL_ARRAY_BUFFER, 0 );
		}

		/*
		 * Enable.
		 */

		glEnableClientState( GL_VERTEX_ARRAY );
		glEnableClientState( GL_COLOR_ARRAY );

		/*
		 * Draw edges.
		 */

		// Enable and set the position attribute.
		glEnableVertexAttribArray( 1 );
		glVertexAttribPointer( 1, VERTEX_SIZE, GL_FLOAT, false, 0, 0 );

		// Edge positions.
		glBindBuffer( GL_ARRAY_BUFFER, vboEdgePositionHandle );
		glVertexPointer( VERTEX_SIZE, GL_FLOAT, 0, 0 );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		// Edge colors.
		glBindBuffer( GL_ARRAY_BUFFER, vboEdgeColorHandle );
		glColorPointer( COLOR_SIZE, GL_FLOAT, 0, 0 );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		// Draw the line segments using the indices.
		glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, iboEdgeIndexHandle );
		glDrawElements( GL_LINES, edgeIndexData.length, GL_UNSIGNED_INT, 0 );
		glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, 0 );

		// Disable the position attribute.
		glDisableVertexAttribArray( 1 );

		/*
		 * Disable.
		 */

		glDisableClientState( GL_COLOR_ARRAY );
		glDisableClientState( GL_VERTEX_ARRAY );
	}

	public void draw( final DataLayout l )
	{
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
