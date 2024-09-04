/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.grapher.opengl.overlays;

import static org.mastodon.views.grapher.display.DataDisplayZoom.TOGGLE_ZOOM;
import static org.mastodon.views.grapher.display.DataDisplayZoom.TOGGLE_ZOOM_KEYS;
import static org.mastodon.views.grapher.display.DataDisplayZoom.ZOOM_GRAPH_OVERLAY_COLOR;

import org.lwjgl.opengl.GL33;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandlerOpenGL;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.model.HasLabel;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.OffsetAxes.OffsetAxesListener;
import org.mastodon.views.grapher.display.ScreenTransformState;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.TransformListener;

/**
 * Drag behaviour that implements a zoom rectangle in a grapher view.
 *
 * @author Jean-Yves Tinevez
 * @param <V>
 *            the type of vertices in the graph.
 * @param <E>
 *            the type of edges in the graph.
 */
public class DataDisplayZoomGL< V extends Vertex< E > & HasTimepoint & HasLabel, E extends Edge< V > >
		extends AbstractNamedBehaviour
		implements DragBehaviour, OffsetAxesListener, TransformListener< ScreenTransform >
{

	public static < V extends Vertex< E > & HasTimepoint & HasLabel, E extends Edge< V > > void
			install( final Behaviours behaviours, final PointCloudPanel panel )
	{
		final DataDisplayZoomGL< V, E > zoom = new DataDisplayZoomGL<>( panel, panel.getTransformEventHandler() );

		// Create and register overlay.
		zoom.transformChanged( panel.getScreenTransform().get() );
		panel.getCanvas().overlays().add( zoom.overlay );
		panel.getScreenTransform().listeners().add( zoom );
		behaviours.namedBehaviour( zoom, TOGGLE_ZOOM_KEYS );
	}


	private final InertialScreenTransformEventHandlerOpenGL transformEventHandler;

	private boolean dragging;

	private int axesWidth;

	private final ScreenTransform screenTransform;

	private final ZoomOverlay overlay;

	private final PointCloudPanel panel;

	private DataDisplayZoomGL( final PointCloudPanel panel, final InertialScreenTransformEventHandlerOpenGL transformEventHandler )
	{
		super( TOGGLE_ZOOM );
		this.panel = panel;
		this.transformEventHandler = transformEventHandler;

		dragging = false;
		screenTransform = new ScreenTransform();
		overlay = new ZoomOverlay();
	}

	@Override
	public void updateAxesSize( final int width, final int height )
	{
		axesWidth = width;
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		synchronized ( screenTransform )
		{
			screenTransform.set( transform );
		}
	}

	@Override
	public void init( final int x, final int y )
	{
		overlay.ox = x;
		overlay.oy = y;
		overlay.ex = x;
		overlay.ey = y;
		dragging = true;
		overlay.paint = true;
		panel.overlayChanged();
	}

	@Override
	public void drag( final int x, final int y )
	{
		if ( dragging )
		{
			overlay.ex = x;
			overlay.ey = y;
			panel.overlayChanged();
		}
	}

	@Override
	public void end( final int x, final int y )
	{
		if ( dragging )
		{
			dragging = false;
			overlay.paint = false;

			final int x1 = Math.min( overlay.ox, overlay.ex ) - axesWidth;
			final int x2 = Math.max( overlay.ox, overlay.ex ) - axesWidth;
			final int y1 = Math.min( overlay.oy, overlay.ey );
			final int y2 = Math.max( overlay.oy, overlay.ey );
			final double[] screen1 = new double[] { x1, y1 };
			final double[] screen2 = new double[] { x2, y2 };
			final double[] layout1 = new double[ 2 ];
			final double[] layout2 = new double[ 2 ];

			screenTransform.applyInverse( layout1, screen1 );
			screenTransform.applyInverse( layout2, screen2 );
			transformEventHandler.zoomTo(
					layout1[ 0 ],
					layout2[ 0 ],
					layout1[ 1 ],
					layout2[ 1 ] );
		}
	}

	private class ZoomOverlay implements GLOverlayRenderer
	{

		public int ey;

		public int ex;

		public int oy;

		public int ox;

		private boolean paint;

		private final ScreenTransform t;

		private final float a, r, g, b;

		public ZoomOverlay()
		{
			paint = false;
			t = new ScreenTransform();
			// Color.
			r = ZOOM_GRAPH_OVERLAY_COLOR.getRed() / 255f;
			g = ZOOM_GRAPH_OVERLAY_COLOR.getGreen() / 255f;
			b = ZOOM_GRAPH_OVERLAY_COLOR.getBlue() / 255f;
			a = ZOOM_GRAPH_OVERLAY_COLOR.getAlpha() / 255f;
		}

		@Override
		public void setCanvasSize( final int width, final int height )
		{}

		@Override
		public void paint()
		{
			if ( !paint )
				return;

			final ScreenTransformState screenTransformState = DataDisplayZoomGL.this.panel.getScreenTransform();
			screenTransformState.get( t );
			final float sox = ( float ) t.screenToLayoutX( ox );
			final float soy = ( float ) t.screenToLayoutY( oy );
			final float sex = ( float ) t.screenToLayoutX( ex );
			final float sey = ( float ) t.screenToLayoutY( ey );

			GL33.glColor4f( r, g, b, a );

			GL33.glBegin( GL33.GL_LINE_LOOP );
			GL33.glVertex2f( sox, soy );
			GL33.glVertex2f( sex, soy );
			GL33.glVertex2f( sex, sey );
			GL33.glVertex2f( sox, sey );
			GL33.glEnd();
		}
	}
}
