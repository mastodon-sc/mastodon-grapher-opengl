package org.mastodon.grapher.opengl;

import javax.swing.JFrame;

import org.mastodon.feature.FeatureModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;

public class MamutViewGrapherOpenGL
{

	private final DataDisplayFrame frame;

	public MamutViewGrapherOpenGL( final MamutAppModel appModel )
	{
		final int nSources = appModel.getSharedBdvData().getSources().size();
		final ModelGraph graph = appModel.getModel().getGraph();
		final FeatureModel featureModel = appModel.getModel().getFeatureModel();
		final SelectionModel< Spot, Link > selection = appModel.getSelectionModel();
		frame = new DataDisplayFrame( nSources, graph, selection, featureModel );
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}

	public JFrame getFrame()
	{
		return frame;
	}

}
