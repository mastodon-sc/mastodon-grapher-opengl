package org.mastodon.grapher.opengl.overlays;

import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.lwjgl.opengl.GL33;
import org.mastodon.collection.RefSet;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.ScreenTransformState;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Behaviour to select vertices and edges inside a bounding box with a mouse
 * drag.
 * <p>
 * The selection happens in layout space, so it also selects vertices inside
 * dense ranges. A vertex is inside the bounding box if its layout coordinate is
 * inside the bounding box.
 */
public class BoxSelectionBehaviour extends AbstractNamedBehaviour implements DragBehaviour, GLOverlayRenderer
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

	private boolean dragging = false;

	private final boolean ignore = false;

	private final boolean addToSelection;

	private final ScreenTransform t;

	private final ScreenTransformState screenTransformState;

	private final PointCloudPanel pointCloudPanel;

	private final ReentrantReadWriteLock lock;

	private final ModelGraph graph;

	private final SelectionModel< Spot, Link > selection;

	private final FocusModel< Spot > focus;

	public BoxSelectionBehaviour(
			final String name,
			final boolean addToSelection,
			final PointCloudPanel pointCloudPanel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final ReentrantReadWriteLock lock )
	{
		super( name );
		this.addToSelection = addToSelection;
		this.pointCloudPanel = pointCloudPanel;
		this.graph = graph;
		this.focus = focus;
		this.selection = selection;
		this.lock = lock;
		this.t = new ScreenTransform();
		this.screenTransformState = pointCloudPanel.getScreenTransform();
		pointCloudPanel.getCanvas().overlays().add( this );
	}

	@Override
	public void init( final int x, final int y )
	{
		screenTransformState.get( t );
		oX = ( float ) t.screenToLayoutX( x );
		oY = ( float ) t.screenToLayoutY( y );
		dragging = false;
//		ignore = x < headerWidth || y > screenTransform.getScreenHeight();
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void drag( final int x, final int y )
	{
		if ( ignore )
			return;

		screenTransformState.get( t );
		eX = ( float ) t.screenToLayoutX( x );
		eY = ( float ) t.screenToLayoutY( y );
		if ( !dragging )
			dragging = true;

		pointCloudPanel.overlayChanged();
	}

	@Override
	public void end( final int x, final int y )
	{
		if ( ignore )
			return;

		if ( dragging )
		{
			dragging = false;
			lock.readLock().lock();
			try
			{
				selectWithin( oX, oY, eX, eY, addToSelection );
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
		else
		{
			selection.clearSelection();
		}
		pointCloudPanel.overlayChanged();
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

	private void selectWithin( final float x1, final float y1, final float x2, final float y2, final boolean addToSelection )
	{
		selection.pauseListeners();

		if ( !addToSelection )
			selection.clearSelection();

		final DataLayoutMaker layout = pointCloudPanel.getDataLayout();
		final RefSet< Spot > vs = layout.getSpotWithin( x1, y1, x2, y2 );
		final Spot vertexRef = graph.vertexRef();
		for ( final Spot v : vs )
		{
			selection.setSelected( v, true );
			for ( final Link e : v.outgoingEdges() )
			{
				final Spot t = e.getTarget( vertexRef );
				if ( vs.contains( t ) )
					selection.setSelected( e, true );
			}
		}

		final Iterator< Spot > it = vs.iterator();
		if ( it.hasNext() )
			focus.focusVertex( it.next() );

		graph.releaseRef( vertexRef );

		selection.resumeListeners();
	}

	public static void install(
			final Behaviours behaviours,
			final PointCloudPanel panel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final ReentrantReadWriteLock lock )
	{
		final BoxSelectionBehaviour boxSelectBehaviour = new BoxSelectionBehaviour( 
				BOX_SELECT, 
				false, 
				panel, 
				graph,
				focus,
				selection,
				lock );
		behaviours.namedBehaviour( boxSelectBehaviour, BOX_SELECT_KEYS );

		final BoxSelectionBehaviour boxAddSelectBehaviour = new BoxSelectionBehaviour(
				BOX_ADD_SELECT,
				true,
				panel,
				graph,
				focus,
				selection,
				lock );
		behaviours.namedBehaviour( boxAddSelectBehaviour, BOX_ADD_SELECT_KEYS );
	}
}
