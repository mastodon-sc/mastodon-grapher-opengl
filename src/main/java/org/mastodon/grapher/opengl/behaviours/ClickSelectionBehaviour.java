package org.mastodon.grapher.opengl.behaviours;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Behaviour to select a vertex with a mouse click.
 * <p>
 * Always selects the vertex closest to the click.
 */
public class ClickSelectionBehaviour extends AbstractSelectionBehaviour implements ClickBehaviour
{
	public static final String CLICK_SELECT = "click selection";

	public static final String CLICK_ADD_SELECT = "click add to selection";

	private static final String[] CLICK_SELECT_KEYS = new String[] { "button1" };

	private static final String[] CLICK_ADD_SELECT_KEYS = new String[] { "shift button1" };

	public ClickSelectionBehaviour(
			final String name,
			final SelectionModel< Spot, Link > selection,
			final FocusModel< Spot > focus,
			final NavigationHandler<Spot, Link> navigationHandler,
			final ModelGraph graph,
			final PointCloudPanel pointCloudPanel,
			final ReentrantReadWriteLock lock,
			final boolean addToSelection )
	{
		super( name, selection, focus, navigationHandler, graph, pointCloudPanel, lock, addToSelection );
	}

	/**
	 * Coordinates of the click in screen space.
	 */
	private float x;
	/**
	 * Coordinates of the click in screen space.
	 */
	private float y;

	@Override
	public void doSelection()
	{
		Spot spot = pointCloudPanel.getDataLayout().getNearestSpot( x, y, screenTransform );
		if ( spot != null )
		{
			if ( addToSelection )
				selection.toggle( spot );
			else
				selection.setSelected( spot, true );
			focus.focusVertex( spot );
			navigationHandler.notifyNavigateToVertex( spot );
			return;
		}
		if (pointCloudPanel.getDataLayout().isPaintEdges())
		{
			Link link = pointCloudPanel.getDataLayout().getNearestLink( x, y, screenTransform );
			if ( link != null )
			{
				if ( addToSelection )
					selection.toggle( link );
				else
					selection.setSelected( link, true );
				navigationHandler.notifyNavigateToEdge( link );
			}
		}
	}

	@Override
	public void click( final int x, final int y )
	{
		updateCoordinates( x, y );
		select();
	}

	private void updateCoordinates( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		this.x = x;
		this.y = y;
	}

	public static void install(
			final Behaviours behaviours,
			final PointCloudPanel panel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler<Spot, Link> navigation,
			final ReentrantReadWriteLock lock )
	{
		final ClickSelectionBehaviour clickSelectionBehaviour = new ClickSelectionBehaviour(
				CLICK_SELECT, selection, focus, navigation, graph, panel, lock, false );
		behaviours.namedBehaviour( clickSelectionBehaviour, CLICK_SELECT_KEYS );

		final ClickSelectionBehaviour clickAddSelectionBehaviour = new ClickSelectionBehaviour(
				CLICK_ADD_SELECT, selection, focus, navigation, graph, panel, lock, true );
		behaviours.namedBehaviour( clickAddSelectionBehaviour, CLICK_ADD_SELECT_KEYS );
	}
}
