package org.mastodon.grapher.opengl;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mastodon.grapher.opengl.overlays.DataPointsOverlay;
import org.mastodon.mamut.model.Spot;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.ScreenTransformState;

import bdv.viewer.TransformListener;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.PainterThread.Paintable;

public class PointCloudPanel extends JPanel implements Paintable, ContextListener< Spot >, TransformListener< ScreenTransform >, LayoutChangeListener
{

	private static final long serialVersionUID = 1L;

	private final PointCloudCanvas canvas;

	private final PainterThread painterThread;

	private final InertialScreenTransformEventHandler transformHandler;

	private final JScrollBar xScrollBar;

	private final JScrollBar yScrollBar;

	/**
	 * If {@code true}, then scroll-bar {@link AdjustmentListener}s ignore
	 * events (when {@link #screenTransform} is changed by means other than the
	 * user dragging the scroll-bar).
	 */
	private boolean ignoreScrollBarChanges;

	/**
	 * Ratio of {@link #xScrollBar} values to layoutX coordinates.
	 */
	private double xScrollScale;

	/**
	 * Ratio of {@link #yScrollBar} values to layoutY coordinates.
	 */
	private double yScrollScale;

	private final ScreenTransformState screenTransform;

	private final DataPointsOverlay dataPointsOverlay;

	private float layoutMinX;

	private float layoutMaxX;

	private float layoutMinY;

	private float layoutMaxY;

	private final DataLayout layout;

	public PointCloudPanel( final DataLayout layout )
	{
		super( new BorderLayout(), false );
		this.layout = layout;
		final int w = 400;
		final int h = 400;
		setPreferredSize( new Dimension( w, h ) );

		// Core canvas and painter thread.
		this.canvas = new PointCloudCanvas();
		this.painterThread = new PainterThread( this );

		// Screen transform.
		this.screenTransform = new ScreenTransformState( new ScreenTransform( -1, 1, -1, 1, w, h ) );
		this.transformHandler = new InertialScreenTransformEventHandler( screenTransform );
		canvas.setTransformEventHandler( transformHandler );
		screenTransform.listeners().add( this );

		// Overlays for the canvas.
		this.dataPointsOverlay = new DataPointsOverlay( layout, transformHandler );
		dataPointsOverlay.getLayoutChangeListeners().add( this );
		canvas.overlays().add( dataPointsOverlay );

		// Bottom axis.
		final JPanel xAxis = new MyXAxisPanel( canvas.t );
		final JPanel yAxis = new MyYAxisPanel( canvas.t );

		// Add main canvas.
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );
		mainPanel.add( canvas, BorderLayout.CENTER );
		mainPanel.add( xAxis, BorderLayout.SOUTH );
		mainPanel.add( yAxis, BorderLayout.WEST );
		add( mainPanel, BorderLayout.CENTER );

		// Add scroll bars.
		xScrollBar = new JScrollBar( JScrollBar.HORIZONTAL );
		yScrollBar = new JScrollBar( JScrollBar.VERTICAL );
		xScrollBar.addAdjustmentListener( new AdjustmentListener()
		{
			@Override
			public void adjustmentValueChanged( final AdjustmentEvent e )
			{
				if ( ignoreScrollBarChanges )
					return;

				final ScreenTransform t = screenTransform.get();
				final double s = xScrollBar.getValue() / xScrollScale;
				t.shiftLayoutX( s - t.getMinX() );
				screenTransform.set( t );
				painterThread.requestRepaint();
			}
		} );
		yScrollBar.addAdjustmentListener( new AdjustmentListener()
		{
			@Override
			public void adjustmentValueChanged( final AdjustmentEvent e )
			{
				if ( ignoreScrollBarChanges )
					return;

				final ScreenTransform t = screenTransform.get();
				final double s = layoutMaxY + layoutMinY - yScrollBar.getValue() / yScrollScale;
				t.shiftLayoutY( ( s - t.getMaxY() ) );
				screenTransform.set( t );
				painterThread.requestRepaint();
			}
		} );

		add( yScrollBar, BorderLayout.EAST );
		final JPanel xScrollPanel = new JPanel( new BorderLayout() );
		xScrollPanel.add( xScrollBar, BorderLayout.CENTER );
		final int space = ( Integer ) UIManager.getDefaults().get( "ScrollBar.width" );
		xScrollPanel.add( Box.createRigidArea( new Dimension( space, 0 ) ), BorderLayout.EAST );
		add( xScrollPanel, BorderLayout.SOUTH );

		painterThread.start();
	}

	@Override
	public void paint()
	{
		repaint();
		SwingUtilities.invokeLater( canvas::render );

		// adjust scrollbars sizes
		xScrollScale = 10000.0 / ( layoutMaxX - layoutMinX + 2 );
		final int xval = ( int ) ( xScrollScale * canvas.t.getMinX() );
		final int xext = ( int ) ( xScrollScale * ( canvas.t.getMaxX() - canvas.t.getMinX() ) );
		final int xmin = ( int ) ( xScrollScale * layoutMinX );
		final int xmax = ( int ) ( xScrollScale * layoutMaxX );
		yScrollScale = 10000.0 / ( layoutMaxY - layoutMinY + 2 );
		final int yext = ( int ) ( yScrollScale * ( canvas.t.getMaxY() - canvas.t.getMinY() ) );
		final int ymin = ( int ) ( yScrollScale * layoutMinY );
		final int ymax = ( int ) ( yScrollScale * layoutMaxY );
		final int yval = ( int ) ( yScrollScale * ( layoutMinY + layoutMaxY - canvas.t.getMaxY() ) );

		ignoreScrollBarChanges = true;
		xScrollBar.setValues( xval, xext, xmin, xmax );
		yScrollBar.setValues( yval, yext, ymin, ymax );
		ignoreScrollBarChanges = false;
	}

	public PointCloudCanvas getCanvas()
	{
		return canvas;
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		canvas.setTransform( transform );
		painterThread.requestRepaint();
	}

	@Override
	public void contextChanged( final Context< Spot > context )
	{
		System.out.println( "Context changed!" ); // DEBUG
	}

	public InertialScreenTransformEventHandler getTransformEventHandler()
	{
		return transformHandler;
	}

	public void stop()
	{
		System.out.println( "Window closing." ); // DEBUG
		painterThread.interrupt();
	}

	public void plot( final FeatureGraphConfig gc )
	{
		layout.setConfig( gc );
		dataPointsOverlay.plot( gc );
	}

	public void updateColor()
	{
		dataPointsOverlay.updateColors();
		paint();
	}

	@Override
	public void layoutChanged( final float layoutMinX, final float layoutMaxX, final float layoutMinY, final float layoutMaxY )
	{
		this.layoutMinX = layoutMinX;
		this.layoutMaxX = layoutMaxX;
		this.layoutMinY = layoutMinY;
		this.layoutMaxY = layoutMaxY;
	}

	private final int axesWidth = 60;

	private final int axesHeight = 40;

	private final int maxTickSpacing = 100;

	// Width of the ticks. TODO put all in a style object.
	private final int tickWidth = 5;

	private class MyYAxisPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final ScreenTransform t;

		public MyYAxisPanel( final ScreenTransform t )
		{
			this.t = t;
			setPreferredSize( new Dimension( axesWidth, axesHeight ) );
		}

		@Override
		protected void paintComponent( final Graphics g )
		{
			final Color bgColor = PointCloudCanvas.BACKGROUND_COLOR;
			final Color fgColor = Color.BLACK;
			final Font tickFont = getFont().deriveFont( getFont().getSize2D() - 2f );
			final Font labelFont = getFont(); // .deriveFont( Font.BOLD );
			final Stroke tickStroke = new BasicStroke();

			final int width = getWidth();
			final boolean isVisibleYAxis = width > 0;
			if ( !isVisibleYAxis )
				return;

			// Erase background.
			final Graphics2D g2 = ( Graphics2D ) g;
			g2.setColor( bgColor );
			final int height = getHeight();
			g2.fillRect( 0, 0, width, height );
			g2.setColor( fgColor );

			// How to center Y ticks on the ticks themselves.
			g2.setFont( tickFont );
			final FontMetrics fm = g2.getFontMetrics( tickFont );
			final int fontAscent = fm.getAscent();
			final int fontInc = fontAscent / 2;

			// Steps.
			final double minY = t.getMinY();
			final double maxY = t.getMaxY();
			double yScale = t.getScaleY();
			yScale = Double.isNaN( yScale ) ? 1. : yScale;
			final int stepY = Math.max( 1, maxTickSpacing / ( int ) ( 1 + yScale ) );
			int ystart = Math.max( 0, ( int ) minY - 1 );
			ystart = ( ystart / stepY ) * stepY;
			int yend = Math.max( 0, 1 + ( int ) maxY );
			yend = ( 1 + yend / stepY ) * stepY;

			// 0. Vertical line.
			g2.setStroke( tickStroke );
			g2.drawLine( width - 1, 0, width - 1, height );

			int maxStringWidth = -1;
			for ( int y = ystart; y <= yend; y = y + stepY )
			{
				// 1. Ticks.
				final int yline = ( int ) t.layoutToScreenY( y );
				g2.drawLine( width - tickWidth, yline, width - 1, yline );

				// 2. Tick labels.
				final int ytext = yline + fontInc;
				final String tickLabel = "" + y;
				final int stringWidth = fm.stringWidth( tickLabel );
				g2.drawString( tickLabel, width - tickWidth - 2 - stringWidth, ytext );
				if ( stringWidth > maxStringWidth )
					maxStringWidth = stringWidth;
			}

			// 3. Y label
			g2.setFont( labelFont );
			final int yLabelWidth = fm.stringWidth( layout.getYLabel() );
			drawStringRotated( g2,
					width - tickWidth - 2 - maxStringWidth - 5,
					height / 2 + yLabelWidth / 2,
					-90.,
					layout.getYLabel() );
		}
	}

	private static final void drawStringRotated( final Graphics2D g2, final double x, final double y, final double angle, final String text )
	{
		g2.translate( ( float ) x, ( float ) y );
		g2.rotate( Math.toRadians( angle ) );
		g2.drawString( text, 0, 0 );
		g2.rotate( -Math.toRadians( angle ) );
		g2.translate( -( float ) x, -( float ) y );
	}

	private class MyXAxisPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		private final ScreenTransform t;

		public MyXAxisPanel( final ScreenTransform t )
		{
			this.t = t;
			setPreferredSize( new Dimension( axesWidth, axesHeight ) );
		}

		@Override
		protected void paintComponent( final Graphics g )
		{
			final Color bgColor = PointCloudCanvas.BACKGROUND_COLOR;
			final Color fgColor = Color.BLACK;
			final Font tickFont = getFont().deriveFont( getFont().getSize2D() - 2f );
			final Font labelFont = getFont(); // .deriveFont( Font.BOLD );
			final Stroke tickStroke = new BasicStroke();

			final int height = getHeight();
			final boolean isVisibleXAxis = height > 0;
			if ( !isVisibleXAxis )
				return;

			final Graphics2D g2 = ( Graphics2D ) g;
			g2.setColor( bgColor );
			final int width = getWidth();
			g2.fillRect( 0, 0, width, height );
			g2.setColor( fgColor );

			// How to center Y ticks on the ticks themselves.
			g2.setFont( tickFont );
			final FontMetrics fm = g2.getFontMetrics( tickFont );
			final int fontAscent = fm.getAscent();

			// Y location of the X axis.
			final int ytop = 0; // Simply 0 in this panel.

			// Steps.
			final double minX = t.getMinX();
			final double maxX = t.getMaxX();
			double xScale = t.getScaleX();
			xScale = Double.isNaN( xScale ) ? 1. : xScale;
			final int stepX = Math.max( 1, maxTickSpacing / ( int ) ( 1 + xScale ) );
			int xstart = Math.max( 0, ( int ) minX - 1 );
			xstart = ( xstart / stepX ) * stepX;
			int xend = Math.max( 0, 1 + ( int ) maxX );
			xend = ( 1 + xend / stepX ) * stepX;

			// From top to bottom.

			// 0. Horizontal line.
			g2.setStroke( tickStroke );
			g2.drawLine( axesWidth, ytop, width, ytop );

			int maxStringWidth = -1;
			for ( int x = xstart; x <= xend; x = x + stepX )
			{
				// 1. Ticks.
				final int xline = ( int ) ( ( x - minX ) * xScale ) + axesWidth;
				if ( xline < axesWidth )
					continue;

				g2.drawLine( xline, ytop + tickWidth, xline, ytop );

				// 2. Tick labels.
				final String tickLabel = "" + x;
				final int stringWidth = fm.stringWidth( tickLabel );
				final int xtext = xline - stringWidth / 2;
				g2.drawString( tickLabel, xtext, ytop + tickWidth + 2 + fontAscent );
				if ( stringWidth > maxStringWidth )
					maxStringWidth = stringWidth;
			}

			// 3. X label
			g2.setFont( labelFont );
			final int xLabelWidth = fm.stringWidth( layout.getXLabel() );
			g2.drawString( layout.getXLabel(),
					axesWidth + ( width - axesWidth ) / 2 - xLabelWidth / 2,
					ytop + tickWidth + 2 + 2 * fontAscent + 5 );

			// 4. Erase bottom left corner.
			g.setColor( bgColor );
			g.fillRect( 0, 0, axesHeight, axesHeight );
		}
	}
}
