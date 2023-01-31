package org.mastodon.grapher.opengl;

import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11C.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;

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
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.mastodon.views.grapher.datagraph.ScreenTransform;

import bdv.TransformEventHandler;

public class PointCloudCanvas extends AWTGLCanvas
{

	private static final long serialVersionUID = 1L;

	private static final int INIT_BUFFER_SIZE = 10; // 10_000;

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	/** The maximal growth step. */
	private final int maximumGrowth = Integer.MAX_VALUE;

	private int framebufferWidth;

	private int framebufferHeight;

	/**
	 * Mouse/Keyboard handler that manipulates the view transformation.
	 */
	private TransformEventHandler handler;

	private final ComponentListener listener = new ComponentAdapter()
	{

		@Override
		public void componentResized( final ComponentEvent e )
		{
			final java.awt.geom.AffineTransform t = PointCloudCanvas.this.getGraphicsConfiguration().getDefaultTransform();
			final float sx = ( float ) t.getScaleX(), sy = ( float ) t.getScaleY();
			framebufferWidth = ( int ) ( getWidth() * sx );
			framebufferHeight = ( int ) ( getHeight() * sy );
			if ( handler != null )
				handler.setCanvasSize( getWidth(), getHeight(), true );
		}
	};

	private FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );

	private FloatBuffer colorBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );

	private int vboVertexHandle;

	private int vboColorHandle;

	private int nPoints = 0;

	private final float pointSize = 5.1f;

	private float[] xyData;

	private float[] colorData;

	private boolean updateXY;

	private boolean updateColor;

	/**
	 * Used to read from the screen transform state.
	 */
	private final ScreenTransform t = new ScreenTransform( -1, 1, -1, 1, 400, 400 );

	public PointCloudCanvas()
	{
		super();
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

	public void putCoords( final float[] xyData )
	{
		this.xyData = xyData;
		this.updateXY = true;
	}

	public void putColors( final float[] colorData )
	{
		this.colorData = colorData;
		this.updateColor = true;
	}

	private void resizeBuffersForNPoints( final int desiredNPoints )
	{
		final int oldCapacity = vertexBuffer.capacity();
		if ( desiredNPoints * VERTEX_SIZE <= oldCapacity )
			return;

		final int growth = Math.min( oldCapacity / 2 + 16, maximumGrowth );
		final int growthNPoints;
		if ( growth > Integer.MAX_VALUE - oldCapacity )
			growthNPoints = Integer.MAX_VALUE / VERTEX_SIZE;
		else
			growthNPoints = ( oldCapacity + growth ) / VERTEX_SIZE;

		final int newNPoints = Math.max( desiredNPoints, growthNPoints );

		// Make new buffers.
		this.vertexBuffer = BufferUtils.createFloatBuffer( newNPoints * VERTEX_SIZE );
		this.colorBuffer = BufferUtils.createFloatBuffer( newNPoints * COLOR_SIZE );

		// Link new buffers.
		this.vboVertexHandle = GL15C.glGenBuffers();
		glBindBuffer( GL_ARRAY_BUFFER, vboVertexHandle );
		glBufferData( GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );
		this.vboColorHandle = GL15C.glGenBuffers();
		glBindBuffer( GL_ARRAY_BUFFER, vboColorHandle );
		glBufferData( GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );
	}

	@Override
	public void initGL()
	{
		createCapabilities();
		
		GL11.glDisable( GL11.GL_DEPTH_TEST );
		GL11.glDisable( GL11.GL_CULL_FACE );

		// Make new buffers.
		this.vertexBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );
		this.colorBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * COLOR_SIZE );

		// Set transparency capabilities.
		GL11.glEnable( GL11.GL_BLEND );
		GL11.glBlendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );

		// New handles.
		this.vboVertexHandle = GL15C.glGenBuffers();
		this.vboColorHandle = GL15C.glGenBuffers();
	}

	@Override
	public void paintGL()
	{
		if ( updateXY )
		{
			if ( xyData.length > vertexBuffer.capacity() )
				resizeBuffersForNPoints( xyData.length / 2 );

			vertexBuffer.limit( xyData.length );
			vertexBuffer.put( xyData );
			vertexBuffer.flip();
			nPoints = xyData.length / 2;
			updateXY = false;
		}
		if ( updateColor )
		{
			colorBuffer.limit( colorData.length );
			colorBuffer.put( colorData );
			colorBuffer.flip();
			updateColor = false;
		}

		GL.createCapabilities();
		glEnableClientState( GL11.GL_VERTEX_ARRAY );
		glEnableClientState( GL_COLOR_ARRAY );

		GL11.glMatrixMode( GL11.GL_PROJECTION );
		GL11.glLoadIdentity();
		GL11.glOrtho( t.getMinX(), t.getMaxX(), t.getMinY(), t.getMaxY(), -1, 1 );
		GL11.glViewport( 0, 0, getFramebufferWidth(), getFramebufferHeight() );
		GL11.glMatrixMode( GL11.GL_MODELVIEW );

		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
		glPointSize( pointSize );

		glBindBuffer( GL15.GL_ARRAY_BUFFER, vboVertexHandle );
		GL15.glBufferSubData( GL15.GL_ARRAY_BUFFER, 0, vertexBuffer );
		GL11.glVertexPointer( VERTEX_SIZE, GL11.GL_FLOAT, 0, 0 );

		glBindBuffer( GL_ARRAY_BUFFER, vboColorHandle );
		GL15.glBufferSubData( GL15.GL_ARRAY_BUFFER, 0, colorBuffer );
		GL15.glColorPointer( COLOR_SIZE, GL11.GL_FLOAT, 0, 0 );

		glDrawArrays( GL11.GL_POINTS, 0, nPoints );
		glBindBuffer( GL15.GL_ARRAY_BUFFER, 0 );

		glDisableClientState( GL_COLOR_ARRAY );
		glDisableClientState( GL_VERTEX_ARRAY );

		swapBuffers();
	}

	private final float[] projectionMatrix = new float[ 16 ];

	public float[] getProjectionMatrix( )
	{
		if ( GL.getCapabilities() == null )
			return new float[] { -1, 1, -1, 1 };
		
		GL11.glGetFloatv( GL11.GL_PROJECTION_MATRIX, projectionMatrix );
		final float xmin = projectionMatrix[ 0 ] * ( -1 ) + projectionMatrix[ 12 ];
		final float xmax = projectionMatrix[ 0 ] * 1 + projectionMatrix[ 12 ];
		final float ymin = projectionMatrix[ 5 ] * ( -1 ) + projectionMatrix[ 13 ];
		final float ymax = projectionMatrix[ 5 ] * 1 + projectionMatrix[ 13 ];
		return new float[] { xmin, xmax, ymin, ymax };
	}

	public int getFramebufferWidth()
	{
		return framebufferWidth;
	}

	public int getFramebufferHeight()
	{
		return framebufferHeight;
	}

	public void setTransform( final ScreenTransform transform )
	{
		t.set( transform );
	}
}
