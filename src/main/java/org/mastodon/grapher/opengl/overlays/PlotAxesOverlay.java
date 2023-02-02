package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.GL11;
import org.mastodon.views.grapher.datagraph.ScreenTransform;

public class PlotAxesOverlay implements GLOverlayRenderer
{

	private int width;

	private int height;

	private final int axesWidth = 50;

	private final int axesHeight = 35;

	private final float[] axesColor = new float[] { 1, 1, 1 };

	private final float lineThickness = 2f;

	private final int maxTickSpacing = 50;

	// Width of the ticks.
	private final int tickWidth = 5;

	private final ScreenTransform t;

	public PlotAxesOverlay( final ScreenTransform t )
	{
		this.t = t;
	}

	@Override
	public void paint()
	{
		GL11.glColor3fv( axesColor );
		GL11.glLineWidth( lineThickness );
		GL11.glBegin( GL11.GL_LINES );

		final double minX = t.getMinX();
		final double maxX = t.getMaxX();
		final double minY = t.getMinY();
		final double maxY = t.getMaxY();

		double xScale = t.getScaleX();
		double yScale = t.getScaleY();
		xScale = Double.isNaN( xScale ) ? 1. : xScale;
		yScale = Double.isNaN( yScale ) ? 1. : yScale;

		final boolean isVisibleYAxis = axesWidth > 0;
		final boolean isVisibleXAxis = axesHeight > 0;


//		final FontMetrics fm = g2.getFontMetrics( style.getAxisTickFont() );
//		g2.setFont( style.getAxisTickFont() );
//		g2.setStroke( style.getAxisStroke() );


		// How to center Y ticks on the ticks themselves.
//		final int fontAscent = fm.getAscent();
		final int fontInc = 1; // fontAscent / 2;

		// Y location of the X axis.
		final int ytop = height - axesHeight;
		final int ybottom = height;

		if ( isVisibleYAxis )
		{
			// Paint axis.

			// Steps.
			final int stepY = Math.max( 1, maxTickSpacing / ( int ) ( 1 + yScale ) );
			int ystart = Math.max( axesHeight, ( int ) minY - 1 );
			ystart = ( ystart / stepY ) * stepY;
			int yend = Math.max( 0, 1 + ( int ) maxY );
			yend = ( 1 + yend / stepY ) * stepY;

			// From right to left.

			// 0. Vertical line.
			GL11.glVertex2f( ( float ) t.screenToLayoutX( axesWidth ), ( float ) t.screenToLayoutY( 0 ) );
			GL11.glVertex2f( ( float ) t.screenToLayoutX( axesWidth ), ( float ) t.screenToLayoutY( height - axesHeight ) );

			final int maxStringWidth = -1;
			for ( int y = ystart; y <= yend; y = y + stepY )
			{
				// 1. Ticks.
				final int yline = ( int ) t.layoutToScreenY( y );
				GL11.glVertex2f( ( float ) t.screenToLayoutX( axesWidth - tickWidth ), y );
				GL11.glVertex2f( ( float ) t.screenToLayoutX( axesWidth ), y );

				// 2. Tick labels.
				final int ytext = yline + fontInc;
				final String tickLabel = "" + y;
//				final int stringWidth = fm.stringWidth( tickLabel );
//				g2.drawString( tickLabel, axesWidth - tickWidth - 2 - stringWidth, ytext );
//				if ( stringWidth > maxStringWidth )
//					maxStringWidth = stringWidth;
			}

			// 3. Y label
//			g2.setFont( style.getAxisLabelFont() );
//			final int yLabelWidth = fm.stringWidth( yLabel );
//			drawStringRotated( g2,
//					axesWidth - tickWidth - 2 - maxStringWidth - 5,
//					height / 2 + yLabelWidth / 2,
//					-90.,
//					yLabel );

		}

		if ( isVisibleXAxis )
		{
			// Steps.
			final int stepX = Math.max( 1, maxTickSpacing / ( int ) ( 1 + xScale ) );
			int xstart = Math.max( 0, ( int ) minX - 1 );
			xstart = ( xstart / stepX ) * stepX;
			int xend = Math.max( 0, 1 + ( int ) maxX );
			xend = ( 1 + xend / stepX ) * stepX;

			// From top to bottom.

			// 0. Horizontal line.
			GL11.glVertex2f( ( float ) t.screenToLayoutX( axesWidth ), ( float ) t.screenToLayoutY( ytop ) );
			GL11.glVertex2f( ( float ) t.screenToLayoutX( width ), ( float ) t.screenToLayoutY( ytop ) );
			
//			g2.setFont( style.getAxisTickFont() );

			final int maxStringWidth = -1;
			for ( int x = xstart; x <= xend; x = x + stepX )
			{
				// 1. Ticks.
				final int xline = ( int ) ( ( x - minX ) * xScale ) + axesWidth;
				GL11.glVertex2f( ( float ) t.screenToLayoutX( xline ), ( float ) t.screenToLayoutY( ytop + tickWidth ) );
				GL11.glVertex2f( ( float ) t.screenToLayoutX( xline ), ( float ) t.screenToLayoutY( ytop ) );

				// 2. Tick labels.
				final String tickLabel = "" + x;
//				final int stringWidth = fm.stringWidth( tickLabel );
//				final int xtext = xline - stringWidth / 2;
//				g2.drawString( tickLabel, xtext, ytop + tickWidth + 2 + fontAscent );
//				if ( stringWidth > maxStringWidth )
//					maxStringWidth = stringWidth;
				
			}

			// 3. X label
//			g2.setFont( style.getAxisLabelFont() );
//			final int xLabelWidth = fm.stringWidth( xLabel );
//			g2.drawString( xLabel,
//					axesWidth + ( width - axesWidth ) / 2 - xLabelWidth / 2,
//					ytop + tickWidth + 2 + 2 * fontAscent + 5 );
		}

		GL11.glEnd();
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

}
