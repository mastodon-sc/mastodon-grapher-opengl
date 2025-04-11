package org.mastodon.grapher.opengl.overlays;

import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.DEFAULT_POINT_SIZE;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;
import org.mastodon.grapher.opengl.DataLayoutMaker;

public class HighlightOverlay implements GLOverlayRenderer
{

	public static final float HIGHLIGHT_LINE_WIDTH = 3f;

	private float[] highlightedVertexCol;

	private float[] highlightedVertexPos;

	private final DataLayoutMaker layout;

	private float[] highlightedVertexBg;

	private float[] highlightedEdgePos0;

	private float[] highlightedEdgePos1;

	private float[] highlightedEdgeCol;

	private float[] highlightedEdgeBg;

	public HighlightOverlay( final DataLayoutMaker layout )
	{
		this.layout = layout;
	}

	@Override
	public void paint()
	{
		if ( highlightedVertexPos != null )
		{
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
		if (highlightedEdgePos0 != null)
		{
			FloatBuffer lineWidth = BufferUtils.createFloatBuffer(1);
			GL33.glGetFloatv(GL33.GL_LINE_WIDTH, lineWidth);
			float currentLineWidth = lineWidth.get(0);
			try
			{
				GL33.glLineWidth( HIGHLIGHT_LINE_WIDTH );
				GL33.glColor4f( highlightedEdgeCol[0], highlightedEdgeCol[1], highlightedEdgeCol[2], highlightedEdgeCol[3] );
				GL33.glBegin( GL33.GL_LINES );
				GL33.glVertex2f( highlightedEdgePos0[ 0 ], highlightedEdgePos0[ 1 ] );
				GL33.glVertex2f( highlightedEdgePos1[ 0 ], highlightedEdgePos1[ 1 ] );
				GL33.glEnd();
			}
			finally
			{
				GL33.glLineWidth( currentLineWidth );
			}
		}


	}

	public void update()
	{
		final float[][] highlightVertexData = layout.getHighlightVertexData();
		if ( highlightVertexData == null )
		{
			highlightedVertexPos = null;
			highlightedVertexCol = null;
			highlightedVertexBg = null;
		}
		else
		{
			highlightedVertexPos = highlightVertexData[ 0 ];
			highlightedVertexCol = highlightVertexData[ 1 ];
			highlightedVertexBg = highlightVertexData[ 2 ];
		}

		if (this.layout.isPaintEdges())
		{
			final float[][] highlightEdgeData = layout.getHighlightEdgeData();
			if ( highlightEdgeData == null )
			{
				highlightedEdgePos0 = null;
				highlightedEdgePos1 = null;
				highlightedEdgeCol = null;
				highlightedEdgeBg = null;
			}
			else
			{
				highlightedEdgePos0 = highlightEdgeData[ 0 ];
				highlightedEdgePos1 = highlightEdgeData[ 1 ];
				highlightedEdgeCol = highlightEdgeData[ 2 ];
				highlightedEdgeBg = highlightEdgeData[ 3 ];
			}
		}
	}
}
