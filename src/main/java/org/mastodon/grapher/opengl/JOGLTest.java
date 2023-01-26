package org.mastodon.grapher.opengl;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

public class JOGLTest
{

	public static void main( final String[] args )
	{
		final GLProfile glprofile = GLProfile.getDefault();
		final GLCapabilities glcapabilities = new GLCapabilities( glprofile );
		final GLCanvas glcanvas = new GLCanvas( glcapabilities );

		glcanvas.addGLEventListener( new GLEventListener()
		{

			@Override
			public void reshape( final GLAutoDrawable glautodrawable, final int x, final int y, final int width, final int height )
			{
				OneTriangle.setup( glautodrawable.getGL().getGL2(), width, height );
			}

			@Override
			public void init( final GLAutoDrawable glautodrawable )
			{}

			@Override
			public void dispose( final GLAutoDrawable glautodrawable )
			{}

			@Override
			public void display( final GLAutoDrawable glautodrawable )
			{
				OneTriangle.render( glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight() );
			}
		} );

		final JFrame jframe = new JFrame( "One Triangle Swing GLCanvas" );
		jframe.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent windowevent )
			{
				jframe.dispose();
				System.exit( 0 );
			}
		} );

		final JTabbedPane tabbedPane = new JTabbedPane();
		final JPanel panel1 = new JPanel();
		panel1.add( new JLabel( "Hi!" ) );
		tabbedPane.addTab( "OpenGL", glcanvas );
		tabbedPane.addTab( "Text", panel1 );

		jframe.getContentPane().add( tabbedPane, BorderLayout.CENTER );
		jframe.setSize( 640, 480 );
		jframe.setVisible( true );
	}
}