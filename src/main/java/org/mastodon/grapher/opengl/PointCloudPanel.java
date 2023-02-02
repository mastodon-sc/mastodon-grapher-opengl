package org.mastodon.grapher.opengl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mastodon.mamut.model.Spot;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.ScreenTransformState;

import bdv.viewer.TransformListener;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.PainterThread.Paintable;

public class PointCloudPanel extends JPanel implements Paintable, ContextListener< Spot >, TransformListener< ScreenTransform >
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

	private ScreenTransformState screenTransform;

	public PointCloudPanel()
	{
		super( new BorderLayout(), false );

		setBackground( Color.BLACK );

		final int w = 400;
		final int h = 400;
		setPreferredSize( new Dimension( w, h ) );

		this.canvas = new PointCloudCanvas();
		this.painterThread = new PainterThread( this );

		this.screenTransform = new ScreenTransformState( new ScreenTransform( -1, 1, -1, 1, w, h ) );
		this.transformHandler = new InertialScreenTransformEventHandler( screenTransform );
		canvas.setTransformEventHandler( transformHandler );
		screenTransform.listeners().add( this );

		add( canvas, BorderLayout.CENTER );

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
				final double s = canvas.layoutMaxY + canvas.layoutMinY - yScrollBar.getValue() / yScrollScale;
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
		SwingUtilities.invokeLater( canvas::render );

		// adjust scrollbars sizes
		xScrollScale = 10000.0 / ( canvas.layoutMaxX - canvas.layoutMinX + 2 );
		final int xval = ( int ) ( xScrollScale * canvas.t.getMinX() );
		final int xext = ( int ) ( xScrollScale * ( canvas.t.getMaxX() - canvas.t.getMinX() ) );
		final int xmin = ( int ) ( xScrollScale * canvas.layoutMinX );
		final int xmax = ( int ) ( xScrollScale * canvas.layoutMaxX );
		yScrollScale = 10000.0 / ( canvas.layoutMaxY - canvas.layoutMinY + 2 );
		final int yext = ( int ) ( yScrollScale * ( canvas.t.getMaxY() - canvas.t.getMinY() ) );
		final int ymin = ( int ) ( yScrollScale * canvas.layoutMinY );
		final int ymax = ( int ) ( yScrollScale * canvas.layoutMaxY );
		final int yval = ( int ) ( yScrollScale * ( canvas.layoutMinY + canvas.layoutMaxY - canvas.t.getMaxY() ) );

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
}
