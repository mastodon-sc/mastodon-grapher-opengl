package org.mastodon.grapher.opengl;

import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.mastodon.grapher.opengl.overlays.GLOverlayRenderer;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.scijava.listeners.Listeners;

import bdv.TransformEventHandler;

public class PointCloudCanvas extends AWTGLCanvas
{

	private static final long serialVersionUID = 1L;

	// TODO move into a style.
	public static final Color BACKGROUND_COLOR = new Color( 204, 204, 204 );

	private int frameBufferWidth;

	private int frameBufferHeight;

	/**
	 * Mouse/Keyboard handler that manipulates the view transformation.
	 */
	private TransformEventHandler handler;

	private final ComponentListener listener = new ComponentAdapter()
	{

		@Override
		public void componentResized( final ComponentEvent e )
		{
			final int w = getWidth();
			final int h = getHeight();
			final java.awt.geom.AffineTransform awtTransform = PointCloudCanvas.this.getGraphicsConfiguration().getDefaultTransform();
			final float sx = ( float ) awtTransform.getScaleX();
			final float sy = ( float ) awtTransform.getScaleY();
			frameBufferWidth = ( int ) ( w * sx );
			frameBufferHeight = ( int ) ( h * sy );
			overlayRenderers.list.forEach( r -> r.setCanvasSize( w, h ) );
			if ( handler != null )
				handler.setCanvasSize( frameBufferWidth, frameBufferHeight, true );
		}
	};

	private final Listeners.List< GLOverlayRenderer > overlayRenderers;

	/**
	 * Used to read from the screen transform state.
	 */
	final ScreenTransform transform;

	public PointCloudCanvas()
	{
		super();
		overlayRenderers = new Listeners.SynchronizedList<>( r -> r.setCanvasSize( getWidth(), getHeight() ) );

		this.transform = new ScreenTransform( -1, 1, -1, 1, 400, 400 );
		this.addComponentListener( listener );
		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				requestFocusInWindow();
			}
		} );
	}

	/**
	 * OverlayRenderers can be added/removed here.
	 * {@link GLOverlayRenderer#paint()} is invoked for each renderer (in
	 * the order they were added).
	 */
	public Listeners< GLOverlayRenderer > overlays()
	{
		return overlayRenderers;
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


	@Override
	public void initGL()
	{
		createCapabilities();
		
		glDisable( GL_DEPTH_TEST );
		glDisable( GL_CULL_FACE );

		// Set transparency capabilities.
		glEnable( GL_BLEND );
		glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );

		overlayRenderers.list.forEach( GLOverlayRenderer::init );
	}

	@Override
	public void paintGL()
	{
		createCapabilities();
		final float[] gbColArr = new float[ 4 ];
		BACKGROUND_COLOR.getComponents( gbColArr );
		glClearColor( gbColArr[ 0 ], gbColArr[ 1 ], gbColArr[ 2 ], gbColArr[ 3 ] );
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

		glMatrixMode( GL_PROJECTION );
		glLoadIdentity();
		glOrtho( transform.getMinX(), transform.getMaxX(), transform.getMinY(), transform.getMaxY(), -1, 1 );
		glViewport( 0, 0, getFrameBufferWidth(), getFrameBufferHeight() );
		glMatrixMode( GL_MODELVIEW );

		overlayRenderers.list.forEach( GLOverlayRenderer::paint );

		swapBuffers();
	}

	public int getFrameBufferWidth()
	{
		return frameBufferWidth;
	}

	public int getFrameBufferHeight()
	{
		return frameBufferHeight;
	}

	public void setTransform( final ScreenTransform transform )
	{
		this.transform.set( transform );
	}
}
