package org.mastodon.grapher.opengl;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutView;
import org.mastodon.mamut.feature.SpotFrameFeature;
import org.mastodon.mamut.feature.SpotQuickMeanIntensityFeature;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.grapher.datagraph.DataContextListener;
import org.mastodon.views.grapher.datagraph.DataEdge;
import org.mastodon.views.grapher.datagraph.DataGraph;
import org.mastodon.views.grapher.datagraph.DataVertex;
import org.mastodon.views.grapher.display.DataDisplayOptions;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureGraphConfig.GraphDataItemsSource;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.style.DataDisplayStyle;
import org.scijava.ui.behaviour.KeyPressedManager;

public class MamutViewOpenGL extends MamutView< DataGraph< Spot, Link >, DataVertex, DataEdge >
{

	private final ContextChooser< Spot > contextChooser;

	private final GraphColorGeneratorAdapter< Spot, Link, DataVertex, DataEdge > coloringAdapter;

	private final PointCloudPanel dataDisplayPanel;

	public MamutViewOpenGL( final MamutAppModel appModel )
	{
		super( appModel,
				new DataGraph< Spot, Link >(
						appModel.getModel().getGraph(),
						appModel.getModel().getGraphIdBimap(),
						appModel.getModel().getGraph().getLock() ),
				new String[] { KeyConfigContexts.GRAPHER } );

		final KeyPressedManager keyPressedManager = appModel.getKeyPressedManager();
		final Model model = appModel.getModel();

		final AutoNavigateFocusModel< DataVertex, DataEdge > navigateFocusModel =
				new AutoNavigateFocusModel<>( focusModel, navigationHandler );

		final DataContextListener< Spot > contextListener = new DataContextListener<>( viewGraph );
		contextChooser = new ContextChooser<>( contextListener );

		final DataDisplayStyle forwardDefaultStyle = appModel.getDataDisplayStyleManager().getForwardDefaultStyle();
		coloringAdapter = new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );
		final DataDisplayOptions options = DataDisplayOptions.options()
				.shareKeyPressedEvents( keyPressedManager )
				.style( forwardDefaultStyle )
				.graphColorGenerator( coloringAdapter );

		final PointCloudFrame< Spot, Link > frame = new PointCloudFrame< Spot, Link >(
				viewGraph,
				appModel.getModel().getFeatureModel(),
				appModel.getSharedBdvData().getSources().size(),
				highlightModel,
				navigateFocusModel,
				selectionModel,
				navigationHandler,
				model,
				groupHandle,
				contextChooser,
				options );
		setFrame( frame );

		dataDisplayPanel = frame.getDataDisplayPanel();

		// If they are available, set some sensible defaults for the feature.
		final FeatureSpecPair spvx = new FeatureSpecPair( SpotFrameFeature.SPEC,
				SpotFrameFeature.SPEC.getProjectionSpecs().iterator().next(), false, false );
		final FeatureSpecPair spvy = new FeatureSpecPair( SpotQuickMeanIntensityFeature.SPEC,
				SpotQuickMeanIntensityFeature.PROJECTION_SPEC, 0, false, false );
		final FeatureGraphConfig gcv =
				new FeatureGraphConfig( spvx, spvy, GraphDataItemsSource.TRACK_OF_SELECTION, true );
		frame.getVertexSidePanel().setGraphConfig( gcv );

		contextListener.setContextListener( dataDisplayPanel );

		dataDisplayPanel.getTransformEventHandler().install( viewBehaviours );

		frame.setVisible( true );
	}

}
