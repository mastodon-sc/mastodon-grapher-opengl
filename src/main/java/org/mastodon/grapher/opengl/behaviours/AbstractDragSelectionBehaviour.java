package org.mastodon.grapher.opengl.behaviours;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.grapher.opengl.overlays.GLOverlayRenderer;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.scijava.ui.behaviour.DragBehaviour;

public abstract class AbstractDragSelectionBehaviour extends AbstractSelectionBehaviour implements DragBehaviour, GLOverlayRenderer
{
	protected boolean dragging = false;

	AbstractDragSelectionBehaviour( final String name, final SelectionModel< Spot, Link > selection, final FocusModel<Spot> focus,
			final NavigationHandler<Spot, Link> navigationHandler,
			final ModelGraph graph, final PointCloudPanel pointCloudPanel,
			final ReentrantReadWriteLock lock, final boolean addToSelection )
	{
		super( name,selection, focus, navigationHandler, graph, pointCloudPanel, lock, addToSelection );
		pointCloudPanel.getCanvas().overlays().add( this );
	}

	protected abstract void doDrag( final int x, final int y );

	protected abstract void doInit( final int x, final int y );

	protected void doEnd(final int x, final int y)
	{
		// do nothing
	}

	@Override
	public void init( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		doInit( x, y );
		dragging = false;
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void drag( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		doDrag( x, y );
		if ( !dragging )
			dragging = true;
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void end( final int x, final int y )
	{
		doEnd( x, y );
		if ( dragging )
		{
			dragging = false;
			lock.readLock().lock();
			try
			{
				select();
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
		pointCloudPanel.overlayChanged();
	}
}
