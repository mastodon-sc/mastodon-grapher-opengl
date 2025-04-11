/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.grapher.opengl.behaviours;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.ui.keymap.KeyConfigScopes;
import org.mastodon.ui.util.RamerDouglasPeucker;
import org.mastodon.views.grapher.datagraph.DataVertex;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Behaviour to select vertices and edges inside a polygon with a mouse
 * drag.
 * <p>
 * The selection happens in layout space, so it also selects vertices inside
 * dense ranges. A vertex is considered inside the polygon if its layout coordinate is
 * inside the polygon.
 */
public class FreeformSelectionBehaviourOpenGL extends AbstractDragSelectionBehaviour
{

	public static final String FREEFORM_SELECTION = "freeform selection";

	public static final String FREEFORM_SELECTION_ADD = "freeform add to selection";

	private static final String[] FREEFORM_SELECTION_KEYS = new String[] { "ctrl button1" };

	private static final String[] FREEFORM_SELECTION_ADD_KEYS = new String[] { "ctrl shift button1" };

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
		final FreeformSelectionBehaviourOpenGL freeformSelectionBehaviourOpenGL = new FreeformSelectionBehaviourOpenGL(
				FREEFORM_SELECTION,
				false,
				panel,
				graph,
				focus,
				selection,
				navigationHandler,
				lock
		);
		behaviours.namedBehaviour( freeformSelectionBehaviourOpenGL, FREEFORM_SELECTION_KEYS );

		final FreeformSelectionBehaviourOpenGL freeformAddSelectionBehaviourOpenGL = new FreeformSelectionBehaviourOpenGL(
				FREEFORM_SELECTION_ADD,
				true,
				panel,
				graph,
				focus,
				selection,
				navigationHandler,
				lock
		);
		behaviours.namedBehaviour( freeformAddSelectionBehaviourOpenGL, FREEFORM_SELECTION_ADD_KEYS );
	}

	private final List< Point > polygon;

	public FreeformSelectionBehaviourOpenGL(
			final String name,
			final boolean addToSelection,
			final PointCloudPanel pointCloudPanel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final NavigationHandler< Spot, Link > navigationHandler,
			final ReentrantReadWriteLock lock
	)
	{
		super( name, selection, focus, navigationHandler, graph, pointCloudPanel, lock, addToSelection );
		this.polygon = new ArrayList<>();
	}

	@Override
	protected void doInit( final int x, final int y )
	{
		polygon.clear();
		polygon.add( new Point( x, y ) );
	}

	@Override
	protected void doDrag( final int x, final int y )
	{
		final Point p = new Point( x, y );
		if ( RamerDouglasPeucker.shouldAddPoint( polygon, p, 0.1 ) )
			polygon.add( p );
		pointCloudPanel.overlayChanged();
	}

	@Override
	protected void doEnd( final int x, final int y )
	{
		polygon.add( new Point( x, y ) );
	}

	@Override
	public void doSelection()
	{
		// Fetch data points in bounding-box.
		final int minX = polygon.stream().mapToInt(p -> p.x).min().orElse(0);
		final int minY = polygon.stream().mapToInt(p -> p.y).min().orElse(0);
		final int maxX = polygon.stream().mapToInt(p -> p.x).max().orElse(Integer.MAX_VALUE);
		final int maxY = polygon.stream().mapToInt(p -> p.y).max().orElse(Integer.MAX_VALUE);
		float layoutX1 = (float) screenTransform.screenToLayoutX(minX );
		float layoutY1 = (float) screenTransform.screenToLayoutY(minY);
		float layoutX2 = (float) screenTransform.screenToLayoutX(maxX );
		float layoutY2 = (float) screenTransform.screenToLayoutY(maxY);
		final RefSet< Spot > spotsWithinBoundingBox = getSpotsWithinBoundingBox( pointCloudPanel.getDataLayout(), layoutX1, layoutY1, layoutX2, layoutY2 );

		// Test if these points are in polygon.
		final Spot vertexRef = graph.vertexRef();
		for ( final Spot spot : spotsWithinBoundingBox )
		{
			if ( isPointInsidePolygon( spot ) )
			{
				selection.setSelected( spot, true );
				// select links, if both source and target are within the polygon
				for ( final Link link : spot.outgoingEdges() )
				{
					final Spot targetSpot = link.getTarget( vertexRef );
					if ( isPointInsidePolygon( targetSpot ) )
						selection.setSelected( link, true );
				}
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

	private boolean isPointInsidePolygon( final Spot point )
	{
		final int n = polygon.size();
		boolean inside = false;

		final double xl = pointCloudPanel.getDataLayout().getXFeatureValue( point );
		final double yl = pointCloudPanel.getDataLayout().getYFeatureValue( point );
		final double xs = screenTransform.layoutToScreenX( xl );
		final double ys = screenTransform.layoutToScreenY( yl );

		for ( int i = 0, j = n - 1; i < n; j = i++ )
		{
			final Point pi = polygon.get( i );
			final Point pj = polygon.get( j );

			if ( ( pi.y > ys ) != ( pj.y > ys ) &&
					( xs < ( pj.x - pi.x ) * ( ys - pi.y ) / ( pj.y - pi.y ) + pi.x ) )
			{
				inside = !inside;
			}
		}
		return inside;
	}

	@Override
	public void paint()
	{
		if ( !dragging )
			return;
		GL33.glColor3f( 1.0f, 0.0f, 0.0f ); // Red color
		GL33.glBegin( GL33.GL_LINE_LOOP );
		for ( final Point point : polygon )
		{
			double layoutX = screenTransform.screenToLayoutX( point.x );
			double layoutY = screenTransform.screenToLayoutY( point.y );
			GL33.glVertex2f( (float) layoutX, (float) layoutY );
		}
		GL33.glEnd();
	}

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.MASTODON, KeyConfigContexts.GRAPHER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( FREEFORM_SELECTION, FREEFORM_SELECTION_KEYS, "Freeform selection in the OpenGL grapher." );
			descriptions.add( FREEFORM_SELECTION_ADD, FREEFORM_SELECTION_ADD_KEYS, "Freeform add to selection in the OpenGL grapher." );
		}
	}
}
