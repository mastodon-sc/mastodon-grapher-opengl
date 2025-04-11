package org.mastodon.grapher.opengl.behaviours;

import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.lwjgl.opengl.GL33;
import org.mastodon.collection.RefSet;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Behaviour to select vertices and edges inside a bounding box with a mouse
 * drag.
 * <p>
 * The selection happens in layout space, so it also selects vertices inside
 * dense ranges. A vertex is inside the bounding box if its layout coordinate is
 * inside the bounding box.
 */
public class BoxSelectionBehaviour extends AbstractDragSelectionBehaviour
{

	public static final String BOX_SELECT = "data box selection";

	public static final String BOX_ADD_SELECT = "data box add to selection";

	private static final String[] BOX_SELECT_KEYS = new String[] { "button1" };

	private static final String[] BOX_ADD_SELECT_KEYS = new String[] { "shift button1" };

	/**
	 * Coordinates where mouse dragging started in layout coords.
	 */
	private float oX, oY;

	/**
	 * Coordinates where mouse dragging currently is in layout coords.
	 */
	private float eX, eY;

	public BoxSelectionBehaviour(
			final String name,
			final boolean addToSelection,
			final PointCloudPanel pointCloudPanel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler<Spot, Link> navigationHandler,
			final ReentrantReadWriteLock lock
	)
	{
		super( name, selection, focus, navigationHandler, graph, pointCloudPanel, lock, addToSelection );
	}

	@Override
	protected void doInit( final int x, final int y )
	{
		oX = ( float ) screenTransform.screenToLayoutX( x );
		oY = ( float ) screenTransform.screenToLayoutY( y );
	}

	@Override
	protected void doDrag(final int x, final int y)
	{
		eX = ( float ) screenTransform.screenToLayoutX( x );
		eY = ( float ) screenTransform.screenToLayoutY( y );
	}

	@Override
	public void paint()
	{
		if ( !dragging )
			return;
		GL33.glColor3f( 1.0f, 0.0f, 0.0f ); // Red color
		GL33.glBegin( GL33.GL_LINE_LOOP );
		GL33.glVertex2f( oX, oY );
		GL33.glVertex2f( eX, oY );
		GL33.glVertex2f( eX, eY );
		GL33.glVertex2f( oX, eY );
		GL33.glEnd();
	}

	@Override
	public void doSelection(  )
	{

		final RefSet< Spot > spotsWithinBoundingBox = getSpotsWithinBoundingBox( pointCloudPanel.getDataLayout(), oX, oY, eX, eY );
		final Spot vertexRef = graph.vertexRef();
		for ( final Spot spot : spotsWithinBoundingBox )
		{
			selection.setSelected( spot, true );
			// select links, if both source and target are within the bounding box
			for ( final Link link : spot.outgoingEdges() )
			{
				final Spot targetSpot = link.getTarget( vertexRef );
				if ( spotsWithinBoundingBox.contains( targetSpot ) )
					selection.setSelected( link, true );
			}
		}

		final Iterator< Spot > it = spotsWithinBoundingBox.iterator();
		if ( it.hasNext() )
		{
			Spot spot = it.next();
			focus.focusVertex( spot );
			navigationHandler.notifyNavigateToVertex( spot );
		}

		graph.releaseRef( vertexRef );
	}

	public static void install(
			final Behaviours behaviours,
			final PointCloudPanel panel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler< Spot, Link > navigationHandler,
			final ReentrantReadWriteLock lock
	)
	{
		final BoxSelectionBehaviour boxSelectBehaviour = new BoxSelectionBehaviour(
				BOX_SELECT,
				false,
				panel,
				graph,
				focus,
				selection,
				navigationHandler,
				lock
		);
		behaviours.namedBehaviour( boxSelectBehaviour, BOX_SELECT_KEYS );

		final BoxSelectionBehaviour boxAddSelectBehaviour = new BoxSelectionBehaviour(
				BOX_ADD_SELECT,
				true,
				panel,
				graph,
				focus,
				selection,
				navigationHandler,
				lock
		);
		behaviours.namedBehaviour( boxAddSelectBehaviour, BOX_ADD_SELECT_KEYS );
	}
}
