package org.mastodon.grapher.opengl;

import java.io.IOException;

import javax.swing.JFrame;

import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestGrapher
{

	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		final Context context = new Context();
		final WindowManager wm = new WindowManager( context );
//		final String projectPath = "/Users/tinevez/Google Drive/Mastodon/Datasets/Remote/FromVlado/mette_e1.mastodon";
		final String projectPath = "../mastodon/samples/drosophila_crop.mastodon";
		final MamutProject project = new MamutProjectIO().load( projectPath );
		wm.getProjectManager().open( project );
		new MainWindow( wm ).setVisible( true );

		final MamutAppModel appModel = wm.getAppModel();

		final MamutViewGrapherOpenGL grapher = new MamutViewGrapherOpenGL( appModel );
		grapher.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}

}
