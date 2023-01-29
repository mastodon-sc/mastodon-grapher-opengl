package org.mastodon.grapher.opengl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import org.scijava.listeners.Listeners;
import org.scijava.listeners.Listeners.List;

import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import bdv.TransformEventHandler;

/**
 * Trying to bridge ImgLib2 display with OpenGL canvas. Adapted from
 * {@code InteractiveDisplayCanvas}
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class InteractiveDisplayOpenGLCanvas extends GLCanvas
{

	private static final long serialVersionUID = -4582399338233161764L;

	/**
	 * Mouse/Keyboard handler that manipulates the view transformation.
	 */
	private TransformEventHandler handler;

	private final Listeners.List< GLEventListener > overlayRenderers;

	/**
	 * Create a new {@code InteractiveDisplayCanvas}.
	 *
	 * @param width
	 *            preferred component width.
	 * @param height
	 *            preferred component height.
	 */
	public InteractiveDisplayOpenGLCanvas( final int width, final int height )
	{
		setPreferredSize( new Dimension( width, height ) );
		setFocusable( true );

		overlayRenderers = new Listeners.SynchronizedList<>( r ->
		{
			r.reshape( this, 0, 0, getWidth(), getHeight() );
		} );

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();
				// NB: Update of overlayRenderers needs to happen before update of handler
				// Otherwise repaint might start before the render target receives the size change.
				overlayRenderers.list.forEach( r -> r.reshape( InteractiveDisplayOpenGLCanvas.this, 0, 0, w, h ) );
				if ( handler != null )
					handler.setCanvasSize( w, h, true );
			}
		} );

		addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				requestFocusInWindow();
			}
		} );
	}

	/**
	 * Add new event handler. Depending on the interfaces implemented by
	 * <code>handler</code> calls {@link Component#addKeyListener(KeyListener)},
	 * {@link Component#addMouseListener(MouseListener)},
	 * {@link Component#addMouseMotionListener(MouseMotionListener)},
	 * {@link Component#addMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param h
	 *            handler to remove
	 */
	public void addHandler( final Object h )
	{
		if ( h instanceof KeyListener )
			addKeyListener( ( KeyListener ) h );

		if ( h instanceof MouseMotionListener )
			addMouseMotionListener( ( MouseMotionListener ) h );

		if ( h instanceof MouseListener )
			addMouseListener( ( MouseListener ) h );

		if ( h instanceof MouseWheelListener )
			addMouseWheelListener( ( MouseWheelListener ) h );

		if ( h instanceof FocusListener )
			addFocusListener( ( FocusListener ) h );
	}

	/**
	 * Remove an event handler. Add new event handler. Depending on the
	 * interfaces implemented by <code>handler</code> calls
	 * {@link Component#removeKeyListener(KeyListener)},
	 * {@link Component#removeMouseListener(MouseListener)},
	 * {@link Component#removeMouseMotionListener(MouseMotionListener)},
	 * {@link Component#removeMouseWheelListener(MouseWheelListener)}.
	 *
	 * @param h
	 *            handler to remove
	 */
	public void removeHandler( final Object h )
	{
		if ( h instanceof KeyListener )
			removeKeyListener( ( KeyListener ) h );

		if ( h instanceof MouseMotionListener )
			removeMouseMotionListener( ( MouseMotionListener ) h );

		if ( h instanceof MouseListener )
			removeMouseListener( ( MouseListener ) h );

		if ( h instanceof MouseWheelListener )
			removeMouseWheelListener( ( MouseWheelListener ) h );

		if ( h instanceof FocusListener )
			removeFocusListener( ( FocusListener ) h );
	}

	/**
	 * Set the {@link TransformEventHandler} that will be notified when
	 * component is resized.
	 *
	 * @param transformEventHandler
	 *            handler to use
	 */
	public void setTransformEventHandler( final TransformEventHandler transformEventHandler )
	{
		if ( handler != null )
			removeHandler( handler );
		handler = transformEventHandler;
		int w = getWidth();
		int h = getHeight();
		if ( w <= 0 || h <= 0 )
		{
			final Dimension preferred = getPreferredSize();
			w = preferred.width;
			h = preferred.height;
		}
		handler.setCanvasSize( w, h, false );
		addHandler( handler );
	}

	public List< GLEventListener > overlays()
	{
		return overlayRenderers;
	}
}
