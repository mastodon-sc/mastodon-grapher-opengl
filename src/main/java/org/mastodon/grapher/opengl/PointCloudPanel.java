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

import org.mastodon.grapher.opengl.DataLayoutMaker.DataLayout;
import org.mastodon.grapher.opengl.handler.MouseHighlightHandler;
import org.mastodon.grapher.opengl.overlays.DataEdgesOverlay;
import org.mastodon.grapher.opengl.overlays.DataPointsOverlay;
import org.mastodon.grapher.opengl.overlays.HighlightOverlay;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationListener;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.InertialScreenTransformEventHandler;
import org.mastodon.views.grapher.display.ScreenTransformState;

import bdv.viewer.TransformListener;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.PainterThread.Paintable;

public class PointCloudPanel extends JPanel implements Paintable, TransformListener< ScreenTransform >, LayoutChangeListener, NavigationListener<Spot, Link>
{

	private static final long serialVersionUID = 1L;

	private final PointCloudCanvas canvas;

	private final PainterThread painterThread;

	private final InertialScreenTransformEventHandlerOpenGL transformHandler;

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

	private final DataLayoutMaker layout;

	private final DataEdgesOverlay dataEdgesOverlay;

	private final HighlightOverlay highlightOverlay;

	private final MinimalNavigationBehaviour navigationBehaviour;

	private final JPanel mainPanel;

	private final JPanel xAxis;

	private final JPanel yAxis;

	private final ModelGraph graph;

	public PointCloudPanel( final DataLayoutMaker layout, final HighlightModel<Spot, Link > highlightModel, final ModelGraph graph )
	{
		super( new BorderLayout(), false );
		this.layout = layout;
		this.graph = graph;
		final int w = 400;
		final int h = 400;
		setPreferredSize( new Dimension( w, h ) );

		// Core canvas and painter thread.
		this.canvas = new PointCloudCanvas( layout.getStyle() );
		this.painterThread = new PainterThread( this );

		// Screen transform.
		this.screenTransform = new ScreenTransformState( new ScreenTransform( -1, 1, -1, 1, w, h ) );
		this.transformHandler = new InertialScreenTransformEventHandlerOpenGL( screenTransform );
		canvas.setTransformEventHandler( transformHandler );
		screenTransform.listeners().add( this );

		// Navigation behaviour.
		navigationBehaviour = new MinimalNavigationBehaviour( transformHandler, 100, 100 );
		navigationBehaviour.navigateToVertex( null,null );

		// Overlays for the canvas.
		this.dataEdgesOverlay = new DataEdgesOverlay( layout );
		this.dataPointsOverlay = new DataPointsOverlay( layout, transformHandler );
		this.highlightOverlay = new HighlightOverlay( layout );
		dataPointsOverlay.getLayoutChangeListeners().add( this );
		canvas.overlays().add( dataEdgesOverlay );
		canvas.overlays().add( dataPointsOverlay );
		canvas.overlays().add( highlightOverlay );

		// Highlight handling.
		final MouseHighlightHandler highlightHandler = new MouseHighlightHandler( layout, highlightModel, screenTransform.get() );
		canvas.addMouseMotionListener( highlightHandler );
		canvas.addMouseListener( highlightHandler );
		screenTransform.listeners().add( highlightHandler );

		// Bottom axis.
		xAxis = new MyXAxisPanel( canvas.transform );
		yAxis = new MyYAxisPanel( canvas.transform );

		// Add main canvas.
		mainPanel = new JPanel();
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
		SwingUtilities.invokeLater( () -> {
			try
			{
				canvas.render();
			}
			catch ( RuntimeException e )
			{
				// ignore
			}
		} );

		// adjust scrollbars sizes
		xScrollScale = 10000.0 / ( layoutMaxX - layoutMinX + 2 );
		final int xval = ( int ) ( xScrollScale * canvas.transform.getMinX() );
		final int xext = ( int ) ( xScrollScale * ( canvas.transform.getMaxX() - canvas.transform.getMinX() ) );
		final int xmin = ( int ) ( xScrollScale * layoutMinX );
		final int xmax = ( int ) ( xScrollScale * layoutMaxX );
		yScrollScale = 10000.0 / ( layoutMaxY - layoutMinY + 2 );
		final int yext = ( int ) ( yScrollScale * ( canvas.transform.getMaxY() - canvas.transform.getMinY() ) );
		final int ymin = ( int ) ( yScrollScale * layoutMinY );
		final int ymax = ( int ) ( yScrollScale * layoutMaxY );
		final int yval = ( int ) ( yScrollScale * ( layoutMinY + layoutMaxY - canvas.transform.getMaxY() ) );

		ignoreScrollBarChanges = true;
		xScrollBar.setValues( xval, xext, xmin, xmax );
		yScrollBar.setValues( yval, yext, ymin, ymax );
		ignoreScrollBarChanges = false;
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint( g );
		if (g.getClipBounds() == null)
			canvas.paint( g, yAxis.getWidth() );
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


	public InertialScreenTransformEventHandlerOpenGL getTransformEventHandler()
	{
		return transformHandler;
	}

	public ScreenTransformState getScreenTransform()
	{
		return screenTransform;
	}

	public void stop()
	{
		System.out.println( "Window closing." ); // DEBUG
		painterThread.interrupt();
	}

	public void plot( final FeatureGraphConfig gc )
	{
		layout.setConfig( gc );
		plot();
	}

	private void plot()
	{
		final DataLayout dataLayout = layout.layout();
		dataPointsOverlay.draw( dataLayout );
		dataEdgesOverlay.draw( dataLayout );
		painterThread.requestRepaint();
	}

	public void updateColor()
	{
		dataPointsOverlay.updateColors();
		dataEdgesOverlay.updateColors();
		painterThread.requestRepaint();
	}

	public void updateHighlight()
	{
		highlightOverlay.update();
		painterThread.requestRepaint();
	}

	public void overlayChanged()
	{
		painterThread.requestRepaint();
	}

	public DataLayoutMaker getDataLayout()
	{
		return layout;
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

	@Override
	public void navigateToVertex( final Spot vertex )
	{
		navigationBehaviour.navigateToVertex( vertex, screenTransform.get() );
	}

	@Override
	public void navigateToEdge( final Link edge )
	{
		final Spot source = edge.getSource( graph.vertexRef() );
		final Spot target = edge.getTarget( graph.vertexRef() );
		navigationBehaviour.navigateToEdge( edge, source, target, screenTransform.get() );
		graph.releaseRef( source );
		graph.releaseRef( target );
	}

	private class MinimalNavigationBehaviour
	{
		private final InertialScreenTransformEventHandler transformEventHandler;

		private final int screenBorderX;

		private final int screenBorderY;

		public MinimalNavigationBehaviour( final InertialScreenTransformEventHandler transformEventHandler,
				final int screenBorderX, final int screenBorderY )
		{
			this.transformEventHandler = transformEventHandler;
			this.screenBorderX = screenBorderX;
			this.screenBorderY = screenBorderY;
		}

		public void navigateToVertex( final Spot v, final ScreenTransform currentTransform )
		{
			if ( v == null )
				return;
			if (currentTransform == null)
				return;

			final double lx = layout.getXFeatureValue( v );
			final double ly = layout.getYFeatureValue( v );

			final double minX = currentTransform.getMinX();
			final double maxX = currentTransform.getMaxX();
			final double minY = currentTransform.getMinY();
			final double maxY = currentTransform.getMaxY();
			final double bx = screenBorderX / currentTransform.getScaleX();
			final double by = screenBorderY / currentTransform.getScaleY();

			double sx = 0;
			if ( lx > maxX - bx )
				sx = lx - maxX + bx;
			else if ( lx < minX + bx )
				sx = lx - minX - bx;
			double sy = 0;
			if ( ly > maxY - by )
				sy = ly - maxY + by;
			else if ( ly < minY + by )
				sy = ly - minY - by;

			if ( sx != 0 || sy != 0 )
			{
				final double cx = ( minX + maxX ) / 2 + sx;
				final double cy = ( minY + maxY ) / 2 + sy;
				transformEventHandler.centerOn( cx, cy );
			}
		}

		public void navigateToEdge( final Link e, final Spot source, final Spot target,
				final ScreenTransform currentTransform )
		{
			if ( e == null )
				return;
			if (currentTransform == null)
				return;

			final double minX = currentTransform.getMinX();
			final double maxX = currentTransform.getMaxX();
			final double minY = currentTransform.getMinY();
			final double maxY = currentTransform.getMaxY();
			final double bx = screenBorderX / currentTransform.getScaleX();
			final double by = screenBorderY / currentTransform.getScaleY();

			final double sourceX = layout.getXFeatureValue( source );
			final double targetX = layout.getXFeatureValue( target );

			final double eMinX = Math.min( sourceX, targetX );
			final double eMaxX = Math.max( sourceX, targetX );
			final double eMinY = layout.getYFeatureValue( source );
			final double eMaxY = layout.getYFeatureValue( target );
			final double lx = 0.5 * ( eMinX + eMaxX );
			final double ly = 0.5 * ( eMinY + eMaxY );

			double sx = 0;
			if ( ( eMaxX - eMinX ) > ( maxX - minX - 2 * bx ) )
				sx = lx - ( minX + maxX ) / 2;
			else if ( eMaxX > maxX - bx )
				sx = eMaxX - maxX + bx;
			else if ( eMinX < minX + bx )
				sx = eMinX - minX - bx;

			double sy = 0;
			if ( ( eMaxY - eMinY ) > ( maxY - minY - 2 * by ) )
				sy = ly - ( minY + maxY ) / 2;
			else if ( eMaxY > maxY - by )
				sy = eMaxY - maxY + by;
			else if ( eMinY < minY + by )
				sy = eMinY - minY - by;

			if ( sx != 0 || sy != 0 )
			{
				final double cx = ( minX + maxX ) / 2 + sx;
				final double cy = ( minY + maxY ) / 2 + sy;
				transformEventHandler.centerOn( cx, cy );
			}
		}
	}

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
			final Color bgColor = layout.getStyle().getBackgroundColor();
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
			String yLabel = layout.getYLabel();
			if (yLabel != null)
			{
				final int yLabelWidth = fm.stringWidth( layout.getYLabel() );
				drawStringRotated( g2,
						width - tickWidth - 2 - maxStringWidth - 5,
						height / 2 + yLabelWidth / 2,
						-90.,
						layout.getYLabel() );
			}
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
			final Color bgColor = layout.getStyle().getBackgroundColor();
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
			String xLabel = layout.getXLabel();
			if (xLabel != null)
			{
				final int xLabelWidth = fm.stringWidth( xLabel );
				g2.drawString( layout.getXLabel(),
						axesWidth + ( width - axesWidth ) / 2 - xLabelWidth / 2,
						ytop + tickWidth + 2 + 2 * fontAscent + 5 );
			}

			// 4. Erase bottom left corner.
			g.setColor( bgColor );
			g.fillRect( 0, 0, axesHeight, axesHeight );
		}
	}
}
