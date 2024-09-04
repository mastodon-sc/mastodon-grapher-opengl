package org.mastodon.grapher.opengl;

import java.io.IOException;

import javax.swing.JFrame;

import org.mastodon.grapher.opengl.mamut.MamutViewGrapherOpenGL;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.views.grapher.MamutViewGrapher;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestGrapherGlVsGrapher
{
	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();
		final String projectPath = "/Users/tinevez/Google Drive/Mastodon/Datasets/Remote/FromVlado/mette_e1.mastodon";
		final ProjectModel projectModel = ProjectLoader.open( projectPath, context, false, true );
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final MamutViewGrapherOpenGL grapherOpenGL = new MamutViewGrapherOpenGL( projectModel );
		grapherOpenGL.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final MamutViewGrapher grapher = new MamutViewGrapher( projectModel );
		grapher.getFrame().setVisible( true );
		grapher.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
}
