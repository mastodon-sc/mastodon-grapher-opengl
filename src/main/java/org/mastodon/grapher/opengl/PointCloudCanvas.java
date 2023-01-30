package org.mastodon.grapher.opengl;

import java.awt.Dimension;
import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.system.MemoryUtil;

public class PointCloudCanvas extends AWTGLCanvas
{
	private static final long serialVersionUID = 1L;

	private static final int NUM_POINTS = 100;

	private static final int POINT_SIZE = 5;

	private static final int FLOAT_SIZE = Float.BYTES;

	private int vbo;

	public PointCloudCanvas()
	{
		super();
		setPreferredSize( new Dimension( 400, 400 ) );
	}

	@Override
	public void initGL()
	{
		GL.createCapabilities();

		// Generate the points
		final FloatBuffer points = BufferUtils.createFloatBuffer( 2 * NUM_POINTS );
		final Random random = new Random();
		for ( int i = 0; i < NUM_POINTS; i++ )
		{
			points.put( random.nextFloat() * 2 - 1 );
			points.put( random.nextFloat() * 2 - 1 );
		}
		points.flip();

		// Create the VBO
		vbo = GL15.glGenBuffers();
		GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, vbo );
		GL15.glBufferData( GL15.GL_ARRAY_BUFFER, points, GL15.GL_STATIC_DRAW );
		MemoryUtil.memFree( points );

		// Enable the vertex attribute
		GL20.glVertexAttribPointer( 0, 2, GL11.GL_FLOAT, false, 2 * FLOAT_SIZE, 0L );
		GL20.glEnableVertexAttribArray( 0 );

		// Set the viewport
		GL11.glViewport( 0, 0, getWidth(), getHeight() );

		// Clear the color buffer
		GL11.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
	}

	@Override
	public void disposeCanvas()
	{
		super.disposeCanvas();
		GL15.glDeleteBuffers( vbo );
	}

	@Override
	public void paintGL()
	{
		GL11.glClear( GL11.GL_COLOR_BUFFER_BIT );
		GL11.glPointSize( POINT_SIZE );
		GL11.glDrawArrays( GL11.GL_POINTS, 0, NUM_POINTS );
	}
}
