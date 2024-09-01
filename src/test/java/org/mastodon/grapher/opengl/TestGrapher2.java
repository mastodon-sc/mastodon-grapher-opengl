package org.mastodon.grapher.opengl;

import java.io.IOException;

import javax.swing.JFrame;

import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestGrapher
{

	public static void main( final String[] args ) throws IOException, SpimDataException
	{
		final Context context = new Context();
//		final String projectPath = "/Users/tinevez/Google Drive/Mastodon/Datasets/Remote/FromVlado/mette_e1.mastodon";
		final String projectPath = "../mastodon/samples/drosophila_crop.mastodon";
		ProjectModel projectModel = ProjectLoader.open( projectPath, context );
		new MainWindow( projectModel ).setVisible( true );

		final MamutViewOpenGL grapher = new MamutViewOpenGL( projectModel );
		grapher.getFrame().setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}

}
