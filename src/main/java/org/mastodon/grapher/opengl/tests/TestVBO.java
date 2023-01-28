package org.mastodon.grapher.opengl.tests;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.swing.JFrame;

import org.mastodon.ui.coloring.ColorMap;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class TestVBO implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener
{

	private static final int nPoints = 1000; // 1_000_000;

	private int[] vertexVBO;

	private int width, height;

	private int[] colorVBO;

	private double panX, panY;

	private boolean panning;

	private int lastX, lastY;

	private double zoom = 1;

	private FloatBuffer vertexData;

	private Random ran;

	public static void main( final String[] args )
	{
		new TestVBO();
	}

	public TestVBO()
	{
		final GLProfile profile = GLProfile.getDefault();
		final GLCapabilities capabilities = new GLCapabilities( profile );
		final GLCanvas canvas = new GLCanvas( capabilities );

		canvas.addMouseListener( this );
		canvas.addMouseMotionListener( this );
		canvas.addMouseWheelListener( this );
		canvas.addGLEventListener( this );

		final JFrame frame = new JFrame( "My OpenGL Example" );
		frame.setSize( 800, 600 );
		frame.add( canvas );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );

		final FPSAnimator animator = new FPSAnimator( canvas, 60 );
		animator.start();
	}

	@Override
	public void init( final GLAutoDrawable drawable )
	{

		// Generate random vertex data
		ran = new Random( 1l );
		vertexData = Buffers.newDirectFloatBuffer( 2 * nPoints );
		final FloatBuffer colorData = FloatBuffer.allocate( 3 * nPoints );

		final ColorMap cm = ColorMap.getColorMap( ColorMap.JET.getName() );
		for ( int i = 0; i < nPoints; i++ )
		{
//			final double x = ran.nextDouble() * 1.8 - 0.9;
//			final double y = ran.nextDouble() * 1.8 - 0.9;
			final float x = ( float ) i / nPoints * 2 - 1;
			final float y = ( float ) Math.cos( 2. * x * Math.PI );
			vertexData.put( x );
			vertexData.put( y );
			final int c = cm.get( ( float ) i / nPoints );
//			final int a = ( c >> 24 ) & 0xFF;
			final int r = ( c >> 16 ) & 0xFF;
			final int g = ( c >> 8 ) & 0xFF;
			final int b = c & 255;
			colorData.put( r / 255f );
			colorData.put( g / 255f );
			colorData.put( b / 255f );
		}
		vertexData.flip();
		colorData.flip();

		// GL class.
		final GL2 gl = drawable.getGL().getGL2();

		// Vertex buffer -> 2D double.
		vertexVBO = new int[ 1 ];
		gl.glGenBuffers( 1, vertexVBO, 0 );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vertexVBO[ 0 ] );
		gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
		gl.glVertexPointer( 2, GL2.GL_FLOAT, 0, 0 );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, vertexData.capacity() * Float.BYTES,
				vertexData, GL2.GL_STATIC_DRAW );

		// Color buffer -> 3D float.
		colorVBO = new int[ 1 ];
		gl.glGenBuffers( 1, colorVBO, 0 );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, colorVBO[ 0 ] );
		gl.glEnableClientState( GL2.GL_COLOR_ARRAY );
		gl.glColorPointer( 3, GL_FLOAT, 0, 0l );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, colorData.capacity() * Float.BYTES,
				colorData, GL.GL_STATIC_DRAW );
	}

	@Override
	public void reshape( final GLAutoDrawable drawable, final int x, final int y, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		final GL2 gl = drawable.getGL().getGL2();
		gl.glViewport( 0, 0, width, height );
	}

	@Override
	public void display( final GLAutoDrawable drawable )
	{
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

//		gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
		gl.glPointSize( 5f );

		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vertexVBO[ 0 ] );

		vertexData.rewind();
		for ( int i = 0; i < nPoints; i++ )
		{
			final double t = System.currentTimeMillis() / 1000.;
			final float x = ( float ) i / nPoints * 2 - 1;
			final float y = ( float ) ( Math.sin( x + t + Math.PI + 5 * t ) * Math.cos( 2. * x * Math.PI + 5 * t ) );
//			final float x = ran.nextFloat() * 1.8f - 0.9f;
//			final float y = ran.nextFloat() * 1.8f - 0.9f;
			vertexData.put( x );
			vertexData.put( y );
		}
		vertexData.flip();
		gl.glBufferSubData( GL2.GL_ARRAY_BUFFER, 0, 2 * nPoints * Buffers.SIZEOF_FLOAT, vertexData );

		// Handle panning.
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();
		gl.glTranslated( panX, panY, 0 );
		gl.glScaled( zoom, zoom, 1 );
		gl.glDrawArrays( GL2.GL_POINTS, 0, nPoints );
	}

	@Override
	public void dispose( final GLAutoDrawable drawable )
	{
		final GL2 gl = drawable.getGL().getGL2();
		gl.glDeleteBuffers( 1, vertexVBO, 0 );
		gl.glDeleteBuffers( 1, colorVBO, 0 );
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		lastX = e.getX();
		lastY = e.getY();
		panning = true;
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		if ( panning )
		{
			panX += ( e.getX() - lastX ) / ( double ) width;
			panY -= ( e.getY() - lastY ) / ( double ) height;
			lastX = e.getX();
			lastY = e.getY();
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		panning = false;
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		zoom += e.getPreciseWheelRotation() * 0.1;
		zoom = Math.max( 0.1, Math.min( zoom, 10 ) );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{}

	@Override
	public void mouseClicked( final MouseEvent e )
	{}

	@Override
	public void mouseEntered( final MouseEvent e )
	{}

	@Override
	public void mouseExited( final MouseEvent e )
	{}

}
