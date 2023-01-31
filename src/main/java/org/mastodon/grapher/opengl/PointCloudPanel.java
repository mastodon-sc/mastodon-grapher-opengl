package org.mastodon.grapher.opengl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
		SwingUtilities.invokeLater( canvas::render );
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
