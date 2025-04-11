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
package org.mastodon.grapher.opengl.handler;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.HighlightModel;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.OffsetAxes.OffsetAxesListener;

import bdv.viewer.TransformListener;

public class MouseHighlightHandler
		implements MouseMotionListener, MouseListener, TransformListener< ScreenTransform >, OffsetAxesListener
{
	private final DataLayoutMaker dataLayoutMaker;

	private final ScreenTransform screenTransform;

	private final HighlightModel< Spot, Link > highlight;

	private boolean mouseInside;

	private int x, y;

	private int screenWidth;

	private int screenHeight;

	public MouseHighlightHandler(
			final DataLayoutMaker dataLayoutMaker,
			final HighlightModel< Spot, Link > highlight,
			ScreenTransform screenTransform )
	{
		this.dataLayoutMaker = dataLayoutMaker;
		this.highlight = highlight;
		this.screenTransform = screenTransform;
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		x = e.getX();
		y = e.getY();
		highlight();
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		x = e.getX();
		y = e.getY();
		highlight();
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		screenTransform.set( transform );
		screenWidth = transform.getScreenWidth();
		screenHeight = transform.getScreenHeight();
		if ( mouseInside )
			highlight();
	}

	@Override
	public void updateAxesSize( final int width, final int height )
	{
	}

	private void highlight()
	{
		if ( x > screenWidth || y > screenHeight )
		{
			highlight.clearHighlight();
		}
		else
		{
			// See if we can find a vertex.
			Spot nearestSpot = dataLayoutMaker.getNearestSpot( x, y, screenTransform );
			if ( nearestSpot != null )
			{
				highlight.highlightVertex( nearestSpot );
				return;
			}
			if (dataLayoutMaker.isPaintEdges())
			{
				// See if we can find an edge.
				Link nearestLink = dataLayoutMaker.getNearestLink( x, y, screenTransform );
				if ( nearestLink != null )
				{
					highlight.highlightEdge( nearestLink );
					return;
				}
			}
			highlight.clearHighlight();
		}


	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{}

	@Override
	public void mouseReleased( final MouseEvent e )
	{}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		mouseInside = true;
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		highlight.clearHighlight();
		mouseInside = false;
	}
}
