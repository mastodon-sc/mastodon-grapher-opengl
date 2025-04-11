package org.mastodon.grapher.opengl;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.UIManager;

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
		try
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();
		// final String projectPath = "/Users/tinevez/Google Drive/Mastodon/Datasets/Remote/FromVlado/mette_e1.mastodon";
		final String projectPath = "D:\\DeepLineage\\Datasets\\Mette Handberg-Thorsager\\embryo02_13-03-15\\e2-ellipsoids-9.mastodon";
		final ProjectModel projectModel = ProjectLoader.open( projectPath, context, false, true );
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final MamutViewGrapherOpenGL grapherOpenGL = projectModel.getWindowManager().createView( MamutViewGrapherOpenGL.class );
		grapherOpenGL.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final MamutViewGrapher grapher = projectModel.getWindowManager().createView( MamutViewGrapher.class );
		grapher.getFrame().setVisible( true );
		grapher.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
}
