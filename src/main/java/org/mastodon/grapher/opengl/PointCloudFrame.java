package org.mastodon.grapher.opengl;

import static org.mastodon.app.MastodonIcons.FEATURES_ICON;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.GroupLocksPanel;
import org.mastodon.app.ui.ViewFrame;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureModel.FeatureModelListener;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.undo.UndoPointMarker;
import org.mastodon.util.FeatureUtils;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.DataDisplayOptions;
import org.mastodon.views.grapher.display.GrapherSidePanel;
import org.mastodon.views.grapher.display.Plotable;
import org.scijava.ui.behaviour.MouseAndKeyHandler;

public class PointCloudFrame extends ViewFrame implements ContextListener<Spot>, Plotable, GraphChangeListener, FocusListener
{
	private static final long serialVersionUID = 1L;

	private final PointCloudPanel dataDisplayPanel;

	private final GrapherSidePanel<Spot, Link> sidePanel;

	public PointCloudFrame(
			final ModelGraph graph,
			final FeatureModel featureModel,
			final int nSources,
			final HighlightModel< Spot, Link > highlight,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler< Spot, Link > navigation,
			final UndoPointMarker undoPointMarker,
			final GroupHandle groupHandle,
			final DataDisplayOptions< Spot, Link > optional )
	{
		super( "OpenGL Grapher" );

		/*
		 * Plot panel.
		 */

		final DataLayoutMaker layout = new DataLayoutMaker( graph, highlight, selection, featureModel, optional );
		dataDisplayPanel = new PointCloudPanel( layout, highlight, graph );
		graph.addGraphChangeListener( this );
		focus.listeners().add( this );
		navigation.listeners().add( dataDisplayPanel );

		// Update color when the selection or style changes.
		optional.values.getStyle().updateListeners().add( () -> dataDisplayPanel.updateColor() );
		selection.listeners().add( () -> dataDisplayPanel.updateColor() );
		selection.listeners().add( () -> dataDisplayPanel.updateHighlight() );
		highlight.listeners().add( () -> dataDisplayPanel.updateHighlight() );

		/*
		 * Side panel.
		 */

		final ContextChooser< Spot > contextChooser = new ContextChooser<>( this );
		sidePanel = new GrapherSidePanel<>( nSources, contextChooser );
		sidePanel.getBtnPlot().addActionListener( e -> {
			dataDisplayPanel.plot( sidePanel.getGraphConfig() );
			dataDisplayPanel.getTransformEventHandler().zoomOutFully();
		} );

		final FeatureModelListener featureModelListener = () -> sidePanel.setFeatures(
				FeatureUtils.collectFeatureMap( featureModel, Spot.class ),
				FeatureUtils.collectFeatureMap( featureModel, Link.class ) );
		featureModel.listeners().add( featureModelListener );
		featureModelListener.featureModelChanged();

		/*
		 * Main panel is a split pane.
		 */

		final JSplitPane mainPanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, sidePanel, dataDisplayPanel );
		mainPanel.setOneTouchExpandable( true );
		mainPanel.setContinuousLayout( true );
		mainPanel.setBorder( null );
		mainPanel.setDividerLocation( 250 );
		mainPanel.setResizeWeight( 0.9 );

		add( mainPanel, BorderLayout.CENTER );

		/*
		 * Top settings bar.
		 */

		final GroupLocksPanel navigationLocksPanel = new GroupLocksPanel( groupHandle );
		settingsPanel.add( navigationLocksPanel );
		settingsPanel.add( Box.createHorizontalGlue() );

		//		final ContextChooserPanel< ? > contextChooserPanel = new ContextChooserPanel<>( contextChooser );
		//		settingsPanel.add( contextChooserPanel );

		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				dataDisplayPanel.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( dataDisplayPanel, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( dataDisplayPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), dataDisplayPanel.getCanvas() );
		dataDisplayPanel.getCanvas().addHandler( mouseAndKeyHandler );
		setIconImages( FEATURES_ICON );

		pack();
		final int x = optional.values.getX();
		final int y = optional.values.getY();
		if ( x <= 0 && y <= 0 )
			setLocationRelativeTo( null );
		else
			setLocation( x, y );
	}

	public GrapherSidePanel<Spot, Link> getVertexSidePanel()
	{
		return sidePanel;
	}

	public PointCloudPanel getDataDisplayPanel()
	{
		return dataDisplayPanel;
	}

	@Override
	public void plot(final ScreenTransform screenTransform )
	{
		dataDisplayPanel.plot( sidePanel.getGraphConfig() );
		if (screenTransform == null)
			dataDisplayPanel.getTransformEventHandler().zoomOutFully();
		else
		{
			dataDisplayPanel.getScreenTransform().set( screenTransform );
			dataDisplayPanel.transformChanged( screenTransform );
		}
	}

	@Override
	public void plot( boolean keepCurrentScreenTransform )
	{
		if ( keepCurrentScreenTransform )
			plot( dataDisplayPanel.getScreenTransform().get() );
		else
			plot( null );
	}

	@Override
	public void contextChanged( final Context< Spot > context )
	{
		dataDisplayPanel.getDataLayout().contextChanged( context );
		sidePanel.getGraphConfig();
		dataDisplayPanel.plot( sidePanel.getGraphConfig() );
	}

	@Override
	public void graphChanged()
	{
		dataDisplayPanel.plot( sidePanel.getGraphConfig() );
	}

	@Override
	public void focusChanged()
	{
		dataDisplayPanel.plot( sidePanel.getGraphConfig() );
	}
}
