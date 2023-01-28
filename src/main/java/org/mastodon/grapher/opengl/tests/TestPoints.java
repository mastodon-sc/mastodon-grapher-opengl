package org.mastodon.grapher.opengl.tests;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryStack;
import org.mastodon.views.grapher.datagraph.ScreenTransform;

public class TestPoints
{

	// The window handle
	private long window;

	public void run()
	{
		System.out.println( "Hello LWJGL " + Version.getVersion() + "!" );

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks( window );
		glfwDestroyWindow( window );

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback( null ).free();
	}

	private void init()
	{
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint( System.err ).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException( "Unable to initialize GLFW" );

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are
									// already the default
		glfwWindowHint( GLFW_VISIBLE, GLFW_FALSE ); // the window will stay
													// hidden after creation
		glfwWindowHint( GLFW_RESIZABLE, GLFW_TRUE ); // the window will be
														// resizable

		// Create the window
		window = glfwCreateWindow( 800, 600, "Hello World!", NULL, NULL );
		if ( window == NULL )
			throw new RuntimeException( "Failed to create the GLFW window" );

		// Setup a key callback. It will be called every time a key is pressed,
		// repeated or released.
		glfwSetKeyCallback( window, ( window, key, scancode, action, mods ) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose( window, true ); // We will detect this
															// in the rendering
															// loop
		} );

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush())
		{
			final IntBuffer pWidth = stack.mallocInt( 1 ); // int*
			final IntBuffer pHeight = stack.mallocInt( 1 ); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize( window, pWidth, pHeight );

			// Get the resolution of the primary monitor
			final GLFWVidMode vidmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );

			// Center the window
			glfwSetWindowPos(
					window,
					( vidmode.width() - pWidth.get( 0 ) ) / 2,
					( vidmode.height() - pHeight.get( 0 ) ) / 2 );
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent( window );
		// Enable v-sync
		glfwSwapInterval( 1 );

		// Make the window visible
		glfwShowWindow( window );

	}

	private float mouseWheelVelocity = 0f;

	private final float speed = 10f;

	private void loop()
	{
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		System.err.println( "GL_VENDOR:     " + GL11.glGetString( GL11.GL_VENDOR ) );
		System.err.println( "GL_RENDERER:   " + GL11.glGetString( GL11.GL_RENDERER ) );
		System.err.println( "GL_VERSION:    " + GL11.glGetString( GL11.GL_VERSION ) );
		System.err.println();

		final int vertices = 30_000_000;
		GL11C.glPointSize( 1.1f );

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

		final int vbo_vertex_handle = GL15C.glGenBuffers();

		glBindBuffer( GL_ARRAY_BUFFER, vbo_vertex_handle );
		glBufferData( GL_ARRAY_BUFFER, vertex_data, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		final int vbo_color_handle = GL15C.glGenBuffers();
		glBindBuffer( GL_ARRAY_BUFFER, vbo_color_handle );
		glBufferData( GL_ARRAY_BUFFER, color_data, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		final ScreenTransform st = new ScreenTransform();
		st.setScreenSize( 800, 600 );
		st.set( -1, 1, -1, 1, 800, 600 );

		// Set the clear color
//		glClearColor( 1.0f, 0.0f, 0.0f, 0.0f );

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.



//		glfwSetInputMode( window, GLFW_CURSOR, GLFW_CURSOR_DISABLED );

		boolean pressed = false;

		final DoubleBuffer xstart = BufferUtils.createDoubleBuffer( 1 );
		final DoubleBuffer ystart = BufferUtils.createDoubleBuffer( 1 );
		final DoubleBuffer x = BufferUtils.createDoubleBuffer( 1 );
		final DoubleBuffer y = BufferUtils.createDoubleBuffer( 1 );
		final double[] screenCoords = new double[2];
		final double[] worldCoords = new double[ 2 ];
		final double[] screenCoordsStart = new double[ 2 ];
		final double[] worldCoordsStart = new double[ 2 ];

		GLFW.glfwSetScrollCallback( window, new GLFWScrollCallback()
		{
			@Override
			public void invoke( final long win, final double dx, final double dy )
			{
				System.out.println( dy );
				mouseWheelVelocity = ( float ) dy;
				System.out.println( mouseWheelVelocity ); // DEBUG
			}
		} );

		while ( !glfwWindowShouldClose( window ) )
		{
			if ( glfwGetMouseButton( window, GLFW_MOUSE_BUTTON_1 ) == GLFW_PRESS )
			{
				if ( !pressed )
				{
					pressed = true;
					glfwGetCursorPos( window, xstart, ystart );
					screenCoordsStart[ 0 ] = xstart.get( 0 );
					screenCoordsStart[ 1 ] = ystart.get( 0 );
					st.applyInverse( worldCoordsStart, screenCoordsStart );
				}
				glfwGetCursorPos( window, x, y );
				screenCoords[ 0 ] = x.get( 0 );
				screenCoords[ 1 ] = y.get( 0 );
				st.applyInverse( worldCoords, screenCoords );
			}
			else
			{
				pressed = false;
			}



			if ( pressed )
			{
				final double dx = worldCoords[ 0 ] - worldCoordsStart[ 0 ];
				final double dy = worldCoords[ 1 ] - worldCoordsStart[ 1 ];
				worldCoordsStart[ 0 ] = worldCoords[ 0 ];
				worldCoordsStart[ 1 ] = worldCoords[ 1 ];

				System.out.println( "Delta X = " + dx + " Delta Y = " + dy );
				GL11.glTranslated( dx, dy, 0f );
			}

			if ( mouseWheelVelocity != 0 )
			{
				GL11.glScaled( 1 + mouseWheelVelocity / speed, 1 + mouseWheelVelocity / speed, 0 );
				mouseWheelVelocity = 0;
			}

			glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT ); // clear the
																	// framebuffer

			glBindBuffer( GL_ARRAY_BUFFER, vbo_vertex_handle );
			glVertexPointer( vertex_size, GL_FLOAT, 0, 0l );

			glBindBuffer( GL_ARRAY_BUFFER, vbo_color_handle );
			glColorPointer( color_size, GL_FLOAT, 0, 0l );

			glEnableClientState( GL_VERTEX_ARRAY );
			glEnableClientState( GL_COLOR_ARRAY );

//			glDrawArrays( GL_TRIANGLES, 0, vertices );
			glDrawArrays( GL11C.GL_POINTS, 0, vertices );

			glDisableClientState( GL_COLOR_ARRAY );
			glDisableClientState( GL_VERTEX_ARRAY );

			glfwSwapBuffers( window ); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public static void main( final String[] args )
	{
		new TestPoints().run();
	}
}
