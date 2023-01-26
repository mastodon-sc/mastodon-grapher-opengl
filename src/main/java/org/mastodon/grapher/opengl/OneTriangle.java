package org.mastodon.grapher.opengl;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.glu.GLU;

public class OneTriangle {

	private static final IntBuffer buffers = IntBuffer.allocate( 2 );

	private static FloatBuffer vertex_data;

	private static IntBuffer vertexArray = IntBuffer.allocate( 1 );

	private static FloatBuffer color_data;

	private static int vertex_size;

	private static int color_size;

	private static int vbo_vertex_handle;

	private static int vertices;

	private static int vbo_color_handle;

	protected static void setup( final GL2 gl2, final int width, final int height )
	{
        gl2.glMatrixMode( GL2.GL_PROJECTION );
        gl2.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        final GLU glu = new GLU();
//        glu.gluOrtho2D( 0.0f, width, 0.0f, height );
		glu.gluOrtho2D( -100f, width + 100f, 1f, height + 100f );

        gl2.glMatrixMode( GL2.GL_MODELVIEW );
        gl2.glLoadIdentity();

        gl2.glViewport( 0, 0, width, height );

		gl2.glPointSize( 1.1f );

		vertices = 3000; // 30_000_000;
		vertex_size = 3; // X, Y
		color_size = 3; // R, G, B

		vertex_data = BufferUtils.createFloatBuffer( vertices * vertex_size );
		color_data = BufferUtils.createFloatBuffer( vertices * color_size );
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

		gl2.glGenVertexArrays( 1, vertexArray );
		gl2.glBindVertexArray( vertexArray.get( 0 ) );
		gl2.glEnableClientState( GL2.GL_VERTEX_ARRAY );

		gl2.glGenBuffers( 2, buffers );

		gl2.glBindBuffer( GL2.GL_ARRAY_BUFFER, buffers.get( 0 ) );
		gl2.glBufferData( GL2.GL_ARRAY_BUFFER, 4 * vertices * 3, vertex_data, GL3.GL_STATIC_DRAW );

		gl2.glBindBuffer( GL2.GL_ARRAY_BUFFER, buffers.get( 1 ) );
		gl2.glBufferData( GL2.GL_ARRAY_BUFFER, 4 * vertices * 3, color_data, GL2.GL_STREAM_DRAW );

		// Specify how data should be sent to the Program.

		// VertexAttribArray 0 corresponds with location 0 in the vertex shader.
		gl2.glEnableVertexAttribArray( 0 );
		gl2.glBindBuffer( GL2.GL_ARRAY_BUFFER, buffers.get( 0 ) );
		gl2.glVertexAttribPointer( 0, 2, GL.GL_FLOAT, false, 0, 0 );

		// VertexAttribArray 1 corresponds with location 1 in the vertex shader.
		gl2.glEnableVertexAttribArray( 1 );
		gl2.glBindBuffer( GL2.GL_ARRAY_BUFFER, buffers.get( 1 ) );
		gl2.glVertexAttribPointer( 1, 3, GL.GL_FLOAT, false, 0, 0 );
		
	}

	protected static void render( final GL2 gl2, final int width, final int height )
	{
		gl2.glClear( GL3.GL_DEPTH_BUFFER_BIT | GL3.GL_COLOR_BUFFER_BIT );

		gl2.glBindBuffer( GL3.GL_ARRAY_BUFFER, buffers.get( 0 ) );
		// After binding the buffer, now WHAT ?

		gl2.glClear( GL.GL_COLOR_BUFFER_BIT );
		gl2.glPointSize( 3f );

		// draw a triangle filling the window
		gl2.glLoadIdentity();
		gl2.glBegin( GL.GL_POINTS );
//		gl2.glBegin( GL.GL_TRIANGLES );


		gl2.glBindVertexArray( vertexArray.get( 0 ) );
		gl2.glDrawArrays( GL.GL_TRIANGLES, 0, vertices );

        gl2.glEnd();
    }
}