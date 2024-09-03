package org.mastodon.grapher.opengl.overlays;

import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.DEFAULT_POINT_SIZE;

import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayoutMaker;

public class HighlightOverlay implements GLOverlayRenderer
{

	private float[] highlightedVertexCol;

	private float[] highlightedVertexPos;

	private final DataLayoutMaker layout;

	private float[] highlightedVertexBg;

	public HighlightOverlay( final DataLayoutMaker layout )
	{
		this.layout = layout;
	}

	@Override
	public void paint()
	{
		if ( highlightedVertexPos == null )
			return;

		final float size = 2 * DEFAULT_POINT_SIZE;

		GL33.glPointSize( size + 2 );
		GL33.glColor4f(
				highlightedVertexBg[ 0 ],
				highlightedVertexBg[ 1 ],
				highlightedVertexBg[ 2 ],
				highlightedVertexBg[ 3 ] );

		GL33.glBegin( GL33.GL_POINTS );
		GL33.glVertex2f( highlightedVertexPos[ 0 ], highlightedVertexPos[ 1 ] );
		GL33.glEnd();

		GL33.glPointSize( size );
		GL33.glColor4f(
				highlightedVertexCol[ 0 ],
				highlightedVertexCol[ 1 ],
				highlightedVertexCol[ 2 ],
				highlightedVertexCol[ 3 ] );

		GL33.glBegin( GL33.GL_POINTS );
		GL33.glVertex2f( highlightedVertexPos[ 0 ], highlightedVertexPos[ 1 ] );
		GL33.glEnd();
	}

	public void update()
	{
		final float[][] out = layout.getHighlightVertexData();
		if ( out == null )
		{
			highlightedVertexPos = null;
			highlightedVertexCol = null;
			highlightedVertexBg = null;
		}
		else
		{
			highlightedVertexPos = out[ 0 ];
			highlightedVertexCol = out[ 1 ];
			highlightedVertexBg = out[ 2 ];
		}
	}
}
