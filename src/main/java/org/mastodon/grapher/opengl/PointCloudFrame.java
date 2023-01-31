package org.mastodon.grapher.opengl;

import static org.mastodon.grapher.opengl.PointCloudCanvas.COLOR_SIZE;

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
import org.mastodon.grouping.GroupHandle;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.coloring.ColorMap;
import org.mastodon.undo.UndoPointMarker;
import org.mastodon.util.FeatureUtils;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.GrapherSidePanel;
import org.scijava.ui.behaviour.MouseAndKeyHandler;

public class PointCloudFrame extends ViewFrame
{
	private static final long serialVersionUID = 1L;

	private final PointCloudPanel dataDisplayPanel;

	private final GrapherSidePanel sidePanel;

	private final DataLayout layout;

	public PointCloudFrame(
			final ModelGraph graph,
			final FeatureModel featureModel,
			final int nSources,
			final HighlightModel< Spot, Link > highlight,
			final FocusModel< Spot, Link > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler< Spot, Link > navigation,
			final UndoPointMarker undoPointMarker,
			final GroupHandle groupHandle,
			final DataDisplayOptions optional )
	{
		super( "Grapher" );
		this.layout = new DataLayout( graph, selection, featureModel );

		/*
		 * Plot panel.
		 */

		dataDisplayPanel = new PointCloudPanel();

		/*
		 * Side panel.
		 */

		final ContextChooser< Spot > contextChooser = new ContextChooser<>( dataDisplayPanel );
		sidePanel = new GrapherSidePanel( nSources, contextChooser );
		sidePanel.btnPlot.addActionListener( e -> plot( sidePanel.getGraphConfig() ) );

		final FeatureModelListener featureModelListener = () -> sidePanel.setFeatures(
				FeatureUtils.collectFeatureMap( featureModel, Spot.class ),
				FeatureUtils.collectFeatureMap( featureModel, Link.class ) );
		featureModel.listeners().add( featureModelListener );
		featureModelListener.featureModelChanged();

		/*
		 * Main panel is a split pane.
		 */

		final JSplitPane mainPanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
				sidePanel, dataDisplayPanel );
		mainPanel.setOneTouchExpandable( true );
		mainPanel.setBorder( null );
		mainPanel.setDividerLocation( 250 );

		add( mainPanel, BorderLayout.CENTER );

		/*
		 * Top settings bar.
		 */

		final GroupLocksPanel navigationLocksPanel = new GroupLocksPanel( groupHandle );
		settingsPanel.add( navigationLocksPanel );
		settingsPanel.add( Box.createHorizontalGlue() );

		//		final ContextChooserPanel< ? > contextChooserPanel = new ContextChooserPanel<>( contextChooser );
		//		settingsPanel.add( contextChooserPanel );

		pack();
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
		SwingUtilities.replaceUIInputMap( dataDisplayPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
				keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), dataDisplayPanel.getCanvas() );
		dataDisplayPanel.getCanvas().addHandler( mouseAndKeyHandler );
		setLocation( optional.values.getX(), optional.values.getY() );
	}

	public GrapherSidePanel getVertexSidePanel()
	{
		return sidePanel;
	}

	public PointCloudPanel getDataDisplayPanel()
	{
		return dataDisplayPanel;
	}

	public void plot( final FeatureGraphConfig graphConfig )
	{
		layout.setConfig( graphConfig );
		final float[] xy = layout.layout();
		
		final int n = xy.length / 2;
		final float[] color = new float[ COLOR_SIZE * n ];

		final ColorMap cm = ColorMap.getColorMap( ColorMap.JET.getName() );
		for ( int i = 0; i < n; i++ )
		{
			final float alpha = ( float ) i / n;
			final int c = cm.get( alpha );
			final int a = ( c >> 24 ) & 0xFF;
			final int r = ( c >> 16 ) & 0xFF;
			final int g = ( c >> 8 ) & 0xFF;
			final int b = c & 255;

			// RGBA
			color[ COLOR_SIZE * i + 0 ] = ( r / 255f );
			color[ COLOR_SIZE * i + 1 ] = ( g / 255f );
			color[ COLOR_SIZE * i + 2 ] = ( b / 255f );
			color[ COLOR_SIZE * i + 3 ] = ( a / 255f );
		}

		dataDisplayPanel.getCanvas().putCoords( xy );
		dataDisplayPanel.getCanvas().putColors( color );
		dataDisplayPanel.getTransformEventHandler().layoutChanged( xy );
	}
}
