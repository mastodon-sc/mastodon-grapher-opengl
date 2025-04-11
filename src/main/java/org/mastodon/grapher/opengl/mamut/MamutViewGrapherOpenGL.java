package org.mastodon.grapher.opengl.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.mamut.MamutMenuBuilder.colorMenu;
import static org.mastodon.mamut.MamutMenuBuilder.colorbarMenu;
import static org.mastodon.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.mamut.MamutMenuBuilder.fileMenu;
import static org.mastodon.mamut.MamutMenuBuilder.tagSetMenu;
import static org.mastodon.mamut.MamutMenuBuilder.viewMenu;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.mastodon.app.IdentityViewGraph;
import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.SearchVertexLabel;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.grapher.opengl.PointCloudCanvas;
import org.mastodon.grapher.opengl.PointCloudFrame;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.grapher.opengl.behaviours.BoxSelectionBehaviour;
import org.mastodon.grapher.opengl.behaviours.ClickSelectionBehaviour;
import org.mastodon.grapher.opengl.overlays.DataDisplayZoomGL;
import org.mastodon.grapher.opengl.behaviours.FreeformSelectionBehaviourOpenGL;
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
import org.mastodon.mamut.views.grapher.GrapherGuiState;
import org.mastodon.ui.ExportViewActions;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.ColorBarOverlay;
import org.mastodon.ui.coloring.ColorBarOverlay.Position;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.ColoringModelMain;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.coloring.HasColorBarOverlay;
import org.mastodon.ui.coloring.HasColoringModel;
import org.mastodon.ui.commandfinder.CommandFinder;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.context.HasContextChooser;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.DataDisplayOptions;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureGraphConfig.GraphDataItemsSource;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.GrapherSidePanel;
import org.mastodon.views.grapher.display.style.DataDisplayStyle;
import org.mastodon.views.grapher.display.style.DataDisplayStyleManager;
import org.scijava.ui.behaviour.KeyPressedManager;

public class MamutViewGrapherOpenGL extends MamutView< ViewGraph< Spot, Link, Spot, Link >, Spot, Link >
		implements HasContextChooser< Spot >, HasColoringModel, HasColorBarOverlay
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
		final DataDisplayOptions< Spot, Link > options = DataDisplayOptions.options();
		options.shareKeyPressedEvents( keyPressedManager )
				.style( forwardDefaultStyle )
				.graphColorGenerator( coloringAdapter );

		final int nSources = appModel.getSharedBdvData().getSources().size();
		final PointCloudFrame frame = new PointCloudFrame(
				model.getGraph(),
				model.getFeatureModel(),
				nSources,
				highlightModel,
				focusModel,
				selectionModel,
				navigationHandler,
				model,
				groupHandle,
				options );
		setFrame( frame );
		dataDisplayPanel = frame.getDataDisplayPanel();

		final FeatureGraphConfig defaultConfig = getDefaultFeatureGraphConfig();
		final GrapherSidePanel<Spot, Link> sidePanel = frame.getVertexSidePanel();
		// Read Feature graph config from GUI state (i.e. restore shown features and show edges setting)
		FeatureGraphConfig config = GrapherGuiState.loadFeatureGraphConfig( sidePanel, guiState, defaultConfig );
		sidePanel.setGraphConfig( config );

//		contextListener.setContextListener( dataDisplayPanel );

		dataDisplayPanel.getTransformEventHandler().install( viewBehaviours );

		// Select with a box.
		BoxSelectionBehaviour.install(
				viewBehaviours,
				dataDisplayPanel,
				model.getGraph(),
				focusModel,
				selectionModel,
				navigationHandler,
				model.getGraph().getLock() );

		// Zoom with a box.
		DataDisplayZoomGL.install( viewBehaviours, dataDisplayPanel );

		// Export the Grapher window to PNG/SVG
		ExportViewActions.install( viewActions, dataDisplayPanel, frame, frame.getTitle() );

		// Select with a polygon.
		FreeformSelectionBehaviourOpenGL.install(
				viewBehaviours,
				dataDisplayPanel,
				model.getGraph(),
				focusModel,
				selectionModel,
				navigationHandler,
				model.getGraph().getLock() );

		// Select with a click
		ClickSelectionBehaviour.install(
				viewBehaviours,
				dataDisplayPanel,
				model.getGraph(),
				focusModel,
				selectionModel,
				navigationHandler,
				model.getGraph().getLock() );

		// Mastodon frame view actions
		MastodonFrameViewActions.install( viewActions, () -> frame );

		/*
		 * Menus
		 */
		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle coloringMenuHandle = new JMenuHandle();
		final JMenuHandle colorbarMenuHandle = new JMenuHandle();
		final JMenuHandle tagSetMenuHandle = new JMenuHandle();

		MainWindow.addMenus( menu, actionMap );
		appModel.getWindowManager().addWindowMenu( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				fileMenu(
						separator(),
						item( ExportViewActions.EXPORT_VIEW_TO_SVG ),
						item( ExportViewActions.EXPORT_VIEW_TO_PNG ) ),
				viewMenu(
						colorMenu( coloringMenuHandle ),
						colorbarMenu( colorbarMenuHandle ),
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
		 * Coloring & colorbar.
		 */
		coloringModel = registerColoring( coloringAdapter, coloringMenuHandle, () -> remapColor() );
		registerTagSetMenu( tagSetMenuHandle, () -> remapColor() );
		colorbarOverlay = new ColorBarOverlay( coloringModel, () -> frame.getVertexSidePanel().getBackground() );
		colorbarOverlay.setVisible( true );
		colorbarOverlay.setPosition( Position.BOTTOM_LEFT );
		registerColorbarOverlay( colorbarOverlay, colorbarMenuHandle, () -> {getFrame().pack(); getFrame().repaint();} );

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
		sideCanvas.setMinimumSize( new Dimension( 250, 1 ) );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.gridx = 0;
		gbc.gridy = 13;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		frame.getVertexSidePanel().add( sideCanvas, gbc );
		colorbarOverlay.setCanvasSize( 250, 80 );

		/*
		 *  Add the command finder.
		 */
		final CommandFinder cf = CommandFinder.build()
				.context( appModel.getContext() )
				.inputTriggerConfig( appModel.getKeymap().getConfig() )
				.keyConfigContexts( keyConfigContexts )
				.descriptionProvider( appModel.getWindowManager().getViewFactories().getCommandDescriptions() )
				.register( viewActions )
				.register( appModel.getModelActions() )
				.register( appModel.getProjectActions() )
				.register( appModel.getPlugins().getPluginActions() )
				.modificationListeners( appModel.getKeymap().updateListeners() )
				.parent( frame )
				.installOn( viewActions );
		cf.getDialog().setTitle( cf.getDialog().getTitle() + " - " + frame.getTitle() );

		/*
		 * Add the search panel.
		 */
		final JPanel searchPanel =
				SearchVertexLabel.install( viewActions, viewGraph, navigationHandler, selectionModel, focusModel, dataDisplayPanel );
		frame.getSettingsPanel().add( searchPanel );

		// update screen transform with actual canvas size before plotting
		ScreenTransform screenTransform = dataDisplayPanel.getScreenTransform().get();
		PointCloudCanvas pointCloudCanvas = dataDisplayPanel.getCanvas();
		int canvasWidth = pointCloudCanvas.getWidth();
		int canvasHeight = pointCloudCanvas.getHeight();
		screenTransform.setScreenSize( canvasWidth, canvasHeight );
		dataDisplayPanel.getScreenTransform().set( screenTransform );

		frame.pack();
		dataDisplayPanel.plot( config );

		dataDisplayPanel.repaint();
		dataDisplayPanel.getCanvas().requestFocusInWindow();
	}

	static FeatureGraphConfig getDefaultFeatureGraphConfig()
	{
		// If they are available, set some sensible defaults for the feature.
		final FeatureSpecPair spvx = new FeatureSpecPair( SpotPositionFeature.SPEC, SpotPositionFeature.PROJECTION_SPECS.get( 0 ), false, false );
		final FeatureSpecPair spvy = new FeatureSpecPair( SpotPositionFeature.SPEC, SpotPositionFeature.PROJECTION_SPECS.get( 1 ), 0, false, false );
		final boolean showEdges = false;
		return new FeatureGraphConfig( spvx, spvy, GraphDataItemsSource.CONTEXT, showEdges );
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

	@Override
	public ContextChooser< Spot > getContextChooser()
	{
		return getFrame().getVertexSidePanel().getContextChooser();
	}

	@Override
	public ColorBarOverlay getColorBarOverlay()
	{
		return this.colorbarOverlay;
	}

	@Override
	public ColoringModel getColoringModel()
	{
		return this.coloringModel;
	}
}
