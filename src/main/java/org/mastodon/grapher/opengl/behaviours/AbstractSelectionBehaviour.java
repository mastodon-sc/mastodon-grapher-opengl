package org.mastodon.grapher.opengl.behaviours;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefSet;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.ScreenTransformState;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;

public abstract class AbstractSelectionBehaviour extends AbstractNamedBehaviour implements SelectionBehaviour
{
	protected final SelectionModel< Spot, Link > selection;

	protected final ScreenTransformState screenTransformState;

	protected final ScreenTransform screenTransform;

	protected final PointCloudPanel pointCloudPanel;

	protected final ReentrantReadWriteLock lock;

	protected final ModelGraph graph;

	protected final FocusModel< Spot > focus;

	protected final NavigationHandler<Spot, Link> navigationHandler;

	protected final boolean addToSelection;

	AbstractSelectionBehaviour( final String name, final SelectionModel< Spot, Link > selection, final FocusModel<Spot> focus,
			final NavigationHandler<Spot, Link> navigationHandler,
			final ModelGraph graph, final PointCloudPanel pointCloudPanel,
			final ReentrantReadWriteLock lock, final boolean addToSelection )
	{
		super( name );
		this.selection = selection;
		this.focus = focus;
		this.navigationHandler = navigationHandler;
		this.graph = graph;
		this.pointCloudPanel = pointCloudPanel;
		this.lock = lock;
		this.addToSelection = addToSelection;
		this.screenTransformState = pointCloudPanel.getScreenTransform();
		this.screenTransform = new ScreenTransform();
	}

	@Override
	public void prepareSelection( )
	{
		selection.pauseListeners();
		if ( !addToSelection )
			selection.clearSelection();
	}

	@Override
	public void finishSelection( )
	{
		selection.resumeListeners();
	}

	protected RefSet< Spot > getSpotsWithinBoundingBox( final DataLayoutMaker layout, final float x1, final float y1, final float x2, final float y2 )
	{
		return layout.getSpotWithin( x1, y1, x2, y2 );
	}
}
