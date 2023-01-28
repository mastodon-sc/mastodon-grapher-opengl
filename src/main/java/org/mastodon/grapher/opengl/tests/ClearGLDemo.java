package org.mastodon.grapher.opengl.tests;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import cleargl.ClearGLDefaultEventListener;
import cleargl.ClearGLDisplayable;
import cleargl.ClearGLWindow;
import cleargl.GLAttribute;
import cleargl.GLProgram;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;


public class ClearGLDemo
{

	private final static float s = 1f;

	public void demo() throws InterruptedException
	{
		final ClearGLDefaultEventListener lClearGLWindowEventListener = new ClearGLDefaultEventListener()
		{
			private GLProgram mGLProgram1;

			private GLAttribute mPosition1, mColor1;

			private GLUniform mProjectionMatrixUniform1,
					mViewMatrixUniform1;

			private GLVertexAttributeArray mPositionAttributeArray1,
					mColorAttributeArray1;

			private GLVertexArray mGLVertexArray1;

			private ClearGLDisplayable mClearGLWindow;

			@Override
			public void init( final GLAutoDrawable pDrawable )
			{
				super.init( pDrawable );
				try
				{
					final GL lGL = pDrawable.getGL();
					lGL.glDisable( GL.GL_DEPTH_TEST );

					mGLProgram1 = GLProgram.buildProgram( lGL,
							ClearGLDemo.class,
							"vertex.glsl",
							"fragment.glsl" );
					System.out.println( mGLProgram1.getProgramInfoLog() );

					mProjectionMatrixUniform1 = mGLProgram1.getUniform( "projMatrix" );
					mViewMatrixUniform1 = mGLProgram1.getUniform( "viewMatrix" );

					mGLVertexArray1 = new GLVertexArray( mGLProgram1 );
					mGLVertexArray1.bind();

					mPosition1 = mGLProgram1.getAttribute( "position" );
					mPositionAttributeArray1 = new GLVertexAttributeArray( mPosition1, 4 );

					mColor1 = mGLProgram1.getAttribute( "color" );
					mColorAttributeArray1 = new GLVertexAttributeArray( mColor1, 4 );

					final int vertices = 30_000_000;
					final int vertex_size = 3; // X, Y
					final int color_size = 3; // R, G, B
					final FloatBuffer vertex_data = BufferUtils.createFloatBuffer( vertices * vertex_size );
					final FloatBuffer color_data = BufferUtils.createFloatBuffer( vertices * color_size );
					final Random rn = new Random();
					for ( int i = 0; i < vertices; i++ )
					{
						vertex_data.put( 1.8f * rn.nextFloat() - 0.9f ); // X
						vertex_data.put( 1.8f * rn.nextFloat() - 0.9f ); // Y
						vertex_data.put( 0f );

						color_data.put( rn.nextFloat() );
						color_data.put( rn.nextFloat() );
						color_data.put( rn.nextFloat() );
					}
					vertex_data.flip();
					color_data.flip();

					mGLVertexArray1.addVertexAttributeArray( mPositionAttributeArray1,
							vertex_data );
					mGLVertexArray1.addVertexAttributeArray( mColorAttributeArray1,
							color_data );
				}
				catch (
						GLException
						| IOException e )
				{
					e.printStackTrace();
				}

			}

			@Override
			public void reshape( final GLAutoDrawable pDrawable,
					final int pX,
					final int pY,
					final int pWidth,
					int pHeight )
			{
				super.reshape( pDrawable,
						pX,
						pY,
						pWidth,
						pHeight );

				if ( pHeight == 0 )
					pHeight = 1;
				final float ratio = ( 1.0f
						* pWidth )
						/ pHeight;
				getClearGLWindow().setPerspectiveProjectionMatrix( 53.13f,
						ratio,
						1.0f,
						30.0f );
//				getClearGLWindow().setOrthoProjectionMatrix( -2,
//						2,
//						-2,
//						2,
//						10,
//						-10 );
			}

			@Override
			public void display( final GLAutoDrawable pDrawable )
			{
				super.display( pDrawable );

				final GL lGL = pDrawable.getGL();

				lGL.glClear( GL.GL_COLOR_BUFFER_BIT
						| GL.GL_DEPTH_BUFFER_BIT );

				getClearGLWindow().lookAt( 0,
						0,
						1,
						0,
						0,
						-1,
						0,
						1,
						0 );

				mGLProgram1.use( lGL );

				mProjectionMatrixUniform1.setFloatMatrix( getClearGLWindow().getProjectionMatrix()
						.getFloatArray(),
						false );
				mViewMatrixUniform1.setFloatMatrix( getClearGLWindow().getViewMatrix()
						.getFloatArray(),
						false );

				mGLVertexArray1.draw( GL.GL_POINTS );

				final int error = lGL.glGetError();
				if ( error != 0 )
				{
					System.err.println( "ERROR on render : "
							+ error );
				}
			}

			@Override
			public void dispose( final GLAutoDrawable pDrawable )
			{
				super.dispose( pDrawable );

				mGLVertexArray1.close();
				mColorAttributeArray1.close();
				mPositionAttributeArray1.close();
				mGLProgram1.close();
			}

			@Override
			public void setClearGLWindow( final ClearGLWindow pClearGLWindow )
			{
				mClearGLWindow = pClearGLWindow;
			}

			@Override
			public ClearGLDisplayable getClearGLWindow()
			{
				return mClearGLWindow;
			}

		};

		lClearGLWindowEventListener.setDebugMode( true );

		try (
				ClearGLDisplayable lClearGLWindow = new ClearGLWindow( "demo: ClearGLWindow",
						512,
						512,
						lClearGLWindowEventListener ))
		{


			lClearGLWindow.addMouseListener( new MouseAdapter()
			{
				@Override
				public void mouseClicked( final com.jogamp.newt.event.MouseEvent e )
				{
					System.out.println( e ); // DEBUG
				};
			} );
			// lClearGLWindow.disableClose();
			lClearGLWindow.setVisible( true );

			while ( lClearGLWindow.isVisible() )
			{
				Thread.sleep( 100 );
			}
		}
	}

	public static void main( final String[] args ) throws InterruptedException
	{
		final AbstractGraphicsDevice lDefaultDevice = GLProfile.getDefaultDevice();
		final GLProfile lProfile = GLProfile.getMaxProgrammable( true );
		final GLCapabilities lCapabilities = new GLCapabilities( lProfile );

		System.out.println( "Device: " + lDefaultDevice );
		System.out.println( "Capabilities: " + lCapabilities );
		System.out.println( "Profile: " + lProfile );

		final ClearGLDemo lClearGLDemo = new ClearGLDemo();
		lClearGLDemo.demo();
	}

}
