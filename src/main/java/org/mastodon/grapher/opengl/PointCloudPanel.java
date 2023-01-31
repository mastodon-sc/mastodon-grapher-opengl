package org.mastodon.grapher.opengl;

import static org.mastodon.grapher.opengl.PointCloudCanvas.COLOR_SIZE;
import static org.mastodon.grapher.opengl.PointCloudCanvas.VERTEX_SIZE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.mastodon.ui.coloring.ColorMap;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.datagraph.DataVertex;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.ScreenTransformState;

import bdv.viewer.TransformListener;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.PainterThread.Paintable;

public class PointCloudPanel extends JPanel implements Paintable, ContextListener< DataVertex >, TransformListener< ScreenTransform >
{

	private static final long serialVersionUID = 1L;

	private final PointCloudCanvas canvas;

	private final PainterThread painterThread;

	private final ScreenTransformState transform;

	private final InertialScreenTransformEventHandler transformHandler;

	public PointCloudPanel()
	{
		super( new BorderLayout(), false );

		setBackground( Color.BLACK );

		final int w = 400;
		final int h = 400;
		setPreferredSize( new Dimension( w, h ) );

		this.transform = new ScreenTransformState( new ScreenTransform( -1, 1, -1, 1, w, h ) );
		transformHandler = new InertialScreenTransformEventHandler( transform );

		this.canvas = new PointCloudCanvas();
		canvas.setTransformEventHandler( transformHandler );
		transform.listeners().add( this );

		this.painterThread = new PainterThread( this );
		painterThread.start();
		add( canvas, BorderLayout.CENTER );
	}

	@Override
	public void paint()
	{
		final double now = System.currentTimeMillis() * 0.01;
		final float width = ( float ) ( Math.sin( now * 0.3 ) );

		final int n = ( int ) ( 10 + Math.abs( width ) * 100 );
		final float[] xy = new float[ VERTEX_SIZE * n ];
		final float[] color = new float[ COLOR_SIZE * n ];

		final ColorMap cm = ColorMap.getColorMap( ColorMap.JET.getName() );
		for ( int i = 0; i < n; i++ )
		{
			final float alpha = ( float ) i / n;

			xy[ 2 * i ] = 1.8f * alpha - 0.9f;
			xy[ 2 * i + 1 ] = ( float ) ( 0.9 * Math.sin( width * 1 * alpha + now / 10 ) * Math.cos( 2 * Math.PI * alpha + 3 * width ) );

			final int c = cm.get( alpha );
			final int a = ( c >> 24 ) & 0xFF;
			final int r = ( c >> 16 ) & 0xFF;
			final int g = ( c >> 8 ) & 0xFF;
			final int b = c & 255;

			// RGBA
			color[ COLOR_SIZE * i + 0 ] = ( r / 255f );
			color[ COLOR_SIZE * i + 1 ] = ( g / 255f );
			color[ COLOR_SIZE * i + 2 ] = ( b / 255f );
			color[ COLOR_SIZE * i + 3 ] = ( a / 255f );
		}

		transformHandler.layoutChanged( xy );
		canvas.putCoords( xy );
		canvas.putColors( color );

		SwingUtilities.invokeLater( () -> canvas.render() );
	}

	public PointCloudCanvas getCanvas()
	{
		return canvas;
	}
	

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		System.out.println( transform ); // DEBUG
		canvas.setTransform( transform );
		painterThread.requestRepaint();
	}

	@Override
	public void contextChanged( final Context< DataVertex > context )
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
