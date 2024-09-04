package org.mastodon.grapher.opengl.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.mamut.MamutMenuBuilder.colorMenu;
import static org.mastodon.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.mamut.MamutMenuBuilder.tagSetMenu;
import static org.mastodon.mamut.MamutMenuBuilder.viewMenu;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.JComponent;

import org.mastodon.app.IdentityViewGraph;
import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.grapher.opengl.DataDisplayOptions;
import org.mastodon.grapher.opengl.PointCloudFrame;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.grapher.opengl.overlays.BoxSelectionBehaviour;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.UndoActions;
import org.mastodon.mamut.feature.SpotPositionFeature;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.branch.BranchLink;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.mamut.views.MamutView;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.ColorBarOverlay;
import org.mastodon.ui.coloring.ColorBarOverlay.Position;
import org.mastodon.ui.coloring.ColoringModelMain;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.coloring.feature.FeatureColorMode;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureGraphConfig.GraphDataItemsSource;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.style.DataDisplayStyle;
import org.mastodon.views.grapher.display.style.DataDisplayStyleManager;
import org.scijava.ui.behaviour.KeyPressedManager;

public class MamutViewGrapherOpenGL extends MamutView< ViewGraph< Spot, Link, Spot, Link >, Spot, Link >
{

	private final GraphColorGeneratorAdapter< Spot, Link, Spot, Link > coloringAdapter;

	private final PointCloudPanel dataDisplayPanel;

	private final ColoringModelMain< Spot, Link, BranchSpot, BranchLink > coloringModel;

	private final ColorBarOverlay colorbarOverlay;

	public MamutViewGrapherOpenGL( final ProjectModel appModel )
	{
		this(appModel, new HashMap<>());
	}
	
	public MamutViewGrapherOpenGL( final ProjectModel appModel, final Map< String, Object > guiState )
	{
		super( appModel,
				createViewGraph( appModel ),
				new String[] { KeyConfigContexts.GRAPHER } );

		final KeyPressedManager keyPressedManager = appModel.getKeyPressedManager();
		final Model model = appModel.getModel();

		final DataDisplayStyleManager dataDisplayStyleManager = appModel.getWindowManager().getManager( DataDisplayStyleManager.class );
		final DataDisplayStyle forwardDefaultStyle = dataDisplayStyleManager.getForwardDefaultStyle();
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
		final FeatureSpecPair spvx = new FeatureSpecPair( SpotPositionFeature.SPEC, SpotPositionFeature.PROJECTION_SPECS.get( 0 ), false, false );
		final FeatureSpecPair spvy = new FeatureSpecPair( SpotPositionFeature.SPEC, SpotPositionFeature.PROJECTION_SPECS.get( 1 ), 0, false, false );
		final boolean showEdges = false;
		final FeatureGraphConfig gcv = new FeatureGraphConfig( spvx, spvy, GraphDataItemsSource.CONTEXT, showEdges );
		frame.getVertexSidePanel().setGraphConfig( gcv );

//		contextListener.setContextListener( dataDisplayPanel );

		dataDisplayPanel.plot( gcv );
		dataDisplayPanel.getTransformEventHandler().zoomTo( -10000, 10000, -10000, 10000 );
		dataDisplayPanel.getTransformEventHandler().install( viewBehaviours );

		BoxSelectionBehaviour.install(
				viewBehaviours,
				dataDisplayPanel,
				model.getGraph(),
				focusModel,
				selectionModel,
				model.getGraph().getLock() );

		/*
		 * Menus
		 */
		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle coloringMenuHandle = new JMenuHandle();
		final JMenuHandle tagSetMenuHandle = new JMenuHandle();

		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				viewMenu(
						colorMenu( coloringMenuHandle ),
						separator(),
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL ) ),
				editMenu(
						item( UndoActions.UNDO ),
						item( UndoActions.REDO ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ),
						item( SelectionActions.SELECT_WHOLE_TRACK ),
						item( SelectionActions.SELECT_TRACK_DOWNWARD ),
						item( SelectionActions.SELECT_TRACK_UPWARD ),
						separator(),
						tagSetMenu( tagSetMenuHandle ) ) );
		appModel.getPlugins().addMenus( menu );

		/*
		 * Coloring & colobar.
		 */
		coloringModel = registerColoring( coloringAdapter, coloringMenuHandle, () -> remapColor() );
		registerTagSetMenu( tagSetMenuHandle, () -> remapColor() );
		colorbarOverlay = new ColorBarOverlay( coloringModel, () -> frame.getVertexSidePanel().getBackground() );
		colorbarOverlay.setVisible( true );
		colorbarOverlay.setPosition( Position.BOTTOM_LEFT );

		// Restore coloring.
		final Boolean noColoring = ( Boolean ) guiState.get( NO_COLORING_KEY );
		if ( null != noColoring && noColoring )
		{
			coloringModel.colorByNone();
		}
		else
		{
			final String tagSetName = ( String ) guiState.get( TAG_SET_KEY );
			final String featureColorModeName = ( String ) guiState.get( FEATURE_COLOR_MODE_KEY );
			if ( null != tagSetName )
			{
				for ( final TagSet tagSet : coloringModel.getTagSetStructure().getTagSets() )
				{
					if ( tagSet.getName().equals( tagSetName ) )
					{
						coloringModel.colorByTagSet( tagSet );
						break;
					}
				}
			}
			else if ( null != featureColorModeName )
			{
				final List< FeatureColorMode > featureColorModes = new ArrayList<>();
				featureColorModes.addAll( coloringModel.getFeatureColorModeManager().getBuiltinStyles() );
				featureColorModes.addAll( coloringModel.getFeatureColorModeManager().getUserStyles() );
				for ( final FeatureColorMode featureColorMode : featureColorModes )
				{
					if ( featureColorMode.getName().equals( featureColorModeName ) )
					{
						coloringModel.colorByFeature( featureColorMode );
						break;
					}
				}
			}
		}

		/*
		 * Add the colorbar to the side panel, by hacking its layout.
		 */

		final JComponent sideCanvas = new JComponent()
		{

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent( final java.awt.Graphics g )
			{
				colorbarOverlay.drawOverlays( g );
			}
		};
		sideCanvas.setPreferredSize( new Dimension( 250, 80 ) );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.gridx = 0;
		gbc.gridy = 13;
		gbc.fill = GridBagConstraints.BOTH;
		frame.getVertexSidePanel().add( sideCanvas, gbc );
		colorbarOverlay.setCanvasSize( 250, 80 );

		frame.setVisible( true );
		dataDisplayPanel.repaint();
		dataDisplayPanel.getCanvas().requestFocusInWindow();

		frame.setSize( 800, 550 );
		frame.setVisible( true );
	}

	@Override
	public PointCloudFrame getFrame()
	{
		return ( PointCloudFrame ) frame;
	}

	private void remapColor()
	{
		dataDisplayPanel.updateColor();
		getFrame().repaint();
	}

	private static ViewGraph< Spot, Link, Spot, Link > createViewGraph( final ProjectModel appModel )
	{
		return IdentityViewGraph.wrap( appModel.getModel().getGraph(), appModel.getModel().getGraphIdBimap() );
	}
}
