package org.mastodon.grapher.opengl;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
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
import cleargl.GLTexture;
import cleargl.GLTypeEnum;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;


public class ClearGLDemo2
{

	private final static float s = 1f;

	private final static float vertices1[] = { 0, 0, 0, 1, 0, s, 0, 1, s, 0, 0, 1 };

	private final static float colors1[] = { 0.0f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			1.0f };

	private final static float vertices2[] = { 0, 0, 0, 1, 0, -s, 0, 1, -s, 0, 0, 1 };

	private final static float texcoord2[] = { 0, 0, 1, 0, 0, 1 };

	private Buffer getTextureBuffer2()
	{
		final int[] lIntArray = new int[ 128 * 128 ];
		for ( int i = 0; i < lIntArray.length; i++ )
			lIntArray[ i ] = 1000 + 128 * i;
		return IntBuffer.wrap( lIntArray );
	}

	private Buffer getTextureBuffer3()
	{
		final float[] lFloatArray = new float[ 1280 * 1280 ];
		for ( int i = 0; i < lFloatArray.length; i++ )
			lFloatArray[ i ] = ( float ) Math.random();
		return FloatBuffer.wrap( lFloatArray );
	}

	public void demo() throws InterruptedException
	{
		final ClearGLDefaultEventListener lClearGLWindowEventListener = new ClearGLDefaultEventListener()
		{
			private GLProgram mGLProgram1,
					mGLProgram2;

			private GLAttribute mPosition1,
					mColor1,
					mPosition2,
					mTexCoord2;

			private GLUniform mProjectionMatrixUniform1,
					mViewMatrixUniform1,
					mProjectionMatrixUniform2,
					mViewMatrixUniform2;

			private GLVertexAttributeArray mPositionAttributeArray1,
					mColorAttributeArray1,
					mPositionAttributeArray2,
					mTexCoordAttributeArray2;

			private GLVertexArray mGLVertexArray1,
					mGLVertexArray2;

			private GLTexture mTexture2;

			private GLUniform mTexUnit2;

			private GLTexture mTexture3;

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
							ClearGLDemo2.class,
							"vertex.glsl",
							"fragment.glsl" );
					System.out.println( mGLProgram1.getProgramInfoLog() );

					mProjectionMatrixUniform1 = mGLProgram1.getUniform( "projMatrix" );
					mViewMatrixUniform1 = mGLProgram1.getUniform( "viewMatrix" );

					mPosition1 = mGLProgram1.getAttribute( "position" );
					mColor1 = mGLProgram1.getAttribute( "color" );

					mGLVertexArray1 = new GLVertexArray( mGLProgram1 );
					mGLVertexArray1.bind();
					mPositionAttributeArray1 = new GLVertexAttributeArray( mPosition1,
							4 );
					mColorAttributeArray1 = new GLVertexAttributeArray( mColor1,
							4 );

					mGLVertexArray1.addVertexAttributeArray( mPositionAttributeArray1,
							Buffers.newDirectFloatBuffer( vertices1 ) );
					mGLVertexArray1.addVertexAttributeArray( mColorAttributeArray1,
							Buffers.newDirectFloatBuffer( colors1 ) );

					mGLProgram2 = GLProgram.buildProgram( lGL,
							ClearGLDemo2.class,
							"vertex.tex.glsl",
							"fragment.tex.glsl" );
					System.out.println( mGLProgram2.getProgramInfoLog() );

					mProjectionMatrixUniform2 = mGLProgram2.getUniform( "projMatrix" );
					mViewMatrixUniform2 = mGLProgram2.getUniform( "viewMatrix" );

					mPosition2 = mGLProgram2.getAttribute( "position" );
					mTexCoord2 = mGLProgram2.getAttribute( "texcoord" );
					mTexUnit2 = mGLProgram2.getUniform( "texUnit" );
					mTexUnit2.setInt( 0 );

					mGLVertexArray2 = new GLVertexArray( mGLProgram2 );
					mGLVertexArray2.bind();
					mPositionAttributeArray2 = new GLVertexAttributeArray( mPosition2,
							4 );
					mTexCoordAttributeArray2 = new GLVertexAttributeArray( mTexCoord2,
							2 );

					mGLVertexArray2.addVertexAttributeArray( mPositionAttributeArray2,
							Buffers.newDirectFloatBuffer( vertices2 ) );
					mGLVertexArray2.addVertexAttributeArray( mTexCoordAttributeArray2,
							Buffers.newDirectFloatBuffer( texcoord2 ) );

					mTexture2 = new GLTexture( mGLProgram2,
							GLTypeEnum.UnsignedByte,
							4,
							128,
							128,
							1,
							true,
							2 );
					mTexture2.copyFrom( getTextureBuffer2() );

					mTexture3 = new GLTexture( mGLProgram2,
							GLTypeEnum.Float,
							1,
							1280,
							1280,
							1,
							true,
							4 );
					mTexture3.copyFrom( getTextureBuffer3() );

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
				// setPerspectiveProjectionMatrix(53.13f,
				// ratio,
				// 1.0f,
				// 30.0f);
				getClearGLWindow().setOrthoProjectionMatrix( -2,
						2,
						-2,
						2,
						10,
						-10 );
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

				mGLVertexArray1.draw( GL.GL_TRIANGLES );

				mGLProgram2.use( lGL );
				mTexture2.bind( mGLProgram2 );

				mProjectionMatrixUniform2.setFloatMatrix( getClearGLWindow().getProjectionMatrix()
						.getFloatArray(),
						false );
				mViewMatrixUniform2.setFloatMatrix( getClearGLWindow().getViewMatrix()
						.getFloatArray(),
						false );

				mGLVertexArray2.draw( GL.GL_TRIANGLES );

				mTexture3.bind( mGLProgram2 );
				getClearGLWindow().getViewMatrix()
						.translate( 0.5f,
								0.5f,
								0 );
				mViewMatrixUniform2.setFloatMatrix( getClearGLWindow().getViewMatrix()
						.getFloatArray(),
						false );
				mGLVertexArray2.draw( GL.GL_TRIANGLES );

				// Check
				// out
				// error
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

				mGLVertexArray2.close();
				mTexCoordAttributeArray2.close();
				mPositionAttributeArray2.close();
				mGLProgram2.close();
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

		final ClearGLDemo2 lClearGLDemo = new ClearGLDemo2();
		lClearGLDemo.demo();
	}

}
