package org.mastodon.grapher.opengl;

import org.mastodon.app.IdentityViewGraph;
import org.mastodon.app.ViewGraph;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutView;
import org.mastodon.mamut.feature.SpotPositionFeature;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureGraphConfig.GraphDataItemsSource;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.style.DataDisplayStyle;
import org.scijava.ui.behaviour.KeyPressedManager;

public class MamutViewOpenGL extends MamutView< ViewGraph< Spot, Link, Spot, Link >, Spot, Link >
{

	private final GraphColorGeneratorAdapter< Spot, Link, Spot, Link > coloringAdapter;

	private final PointCloudPanel dataDisplayPanel;

	public MamutViewOpenGL( final MamutAppModel appModel )
	{
		super( appModel,
				createViewGraph( appModel ),
				new String[] { KeyConfigContexts.GRAPHER } );

		final KeyPressedManager keyPressedManager = appModel.getKeyPressedManager();
		final Model model = appModel.getModel();

		final AutoNavigateFocusModel< Spot, Link > navigateFocusModel =
				new AutoNavigateFocusModel<>( focusModel, navigationHandler );

		final DataDisplayStyle forwardDefaultStyle = appModel.getDataDisplayStyleManager().getForwardDefaultStyle();
		coloringAdapter = new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );
		final DataDisplayOptions options = DataDisplayOptions.options()
				.shareKeyPressedEvents( keyPressedManager )
				.style( forwardDefaultStyle )
				.graphColorGenerator( coloringAdapter );

		final int nSources = appModel.getSharedBdvData().getSources().size();
		final PointCloudFrame frame = new PointCloudFrame(
				model.getGraph(),
				model.getFeatureModel(),
				nSources,
				highlightModel,
				null,
				selectionModel,
				null,
				model,
				groupHandle,
				options );
		setFrame( frame );

		dataDisplayPanel = frame.getDataDisplayPanel();

		// If they are available, set some sensible defaults for the feature.
		final FeatureSpecPair spvx = new FeatureSpecPair( SpotPositionFeature.SPEC,
				SpotPositionFeature.PROJECTION_SPECS.get( 0 ), false, false );
		final FeatureSpecPair spvy = new FeatureSpecPair( SpotPositionFeature.SPEC,
				SpotPositionFeature.PROJECTION_SPECS.get( 1 ), 0, false, false );
		final FeatureGraphConfig gcv =
				new FeatureGraphConfig( spvx, spvy, GraphDataItemsSource.CONTEXT, true );
		frame.getVertexSidePanel().setGraphConfig( gcv );

//		contextListener.setContextListener( dataDisplayPanel );

		frame.plot( gcv );
		dataDisplayPanel.getTransformEventHandler().zoomTo( -10000, 10000, -10000, 10000 );
		dataDisplayPanel.getTransformEventHandler().install( viewBehaviours );

		frame.setVisible( true );
	}

	private static ViewGraph< Spot, Link, Spot, Link > createViewGraph( final MamutAppModel appModel )
	{
		return IdentityViewGraph.wrap( appModel.getModel().getGraph(), appModel.getModel().getGraphIdBimap() );
	}
}
