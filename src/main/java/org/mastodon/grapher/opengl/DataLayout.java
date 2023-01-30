/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.grapher.opengl;

import java.nio.FloatBuffer;
import java.util.Collection;

import org.mastodon.feature.FeatureProjection;
import org.mastodon.graph.Edges;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;

import com.jogamp.common.nio.Buffers;

public class DataLayout
{

	private FeatureProjection< Spot > ypVertex;

	private FeatureProjection< Spot > xpVertex;

	private FeatureProjection< Link > xpEdge;

	private FeatureProjection< Link > ypEdge;

	private boolean incomingEdge;

	private boolean paintEdges;

	public void setXFeatureVertex( final FeatureProjection< Spot > xproj )
	{
		this.xpVertex = xproj;
		this.xpEdge = null;
	}

	public void setYFeatureVertex( final FeatureProjection< Spot > yproj )
	{
		this.ypVertex = yproj;
		this.ypEdge = null;
	}

	public void setXFeatureEdge( final FeatureProjection< Link > xproj, final boolean incoming )
	{
		this.xpEdge = xproj;
		this.incomingEdge = incoming;
		this.xpVertex = null;
	}

	public void setYFeatureEdge( final FeatureProjection< Link > yproj, final boolean incoming )
	{
		this.ypEdge = yproj;
		this.incomingEdge = incoming;
		this.ypVertex = null;
	}

	/**
	 * Sets whether the screen edges will be generated.
	 * 
	 * @param paintEdges
	 *            if <code>true</code> the screen edges will be generated.
	 */
	public void setPaintEdges( final boolean paintEdges )
	{
		this.paintEdges = paintEdges;
	}

	/**
	 * Resets X and Y position based on the current feature specifications for
	 * the current vertices in the data graph.
	 * 
	 * @return
	 */
	public FloatBuffer layout( final Collection< Spot > vertices )
	{
		if ( vertices.isEmpty() )
		{
			return Buffers.newDirectFloatBuffer( 0 );
		}

		final int nPoints = vertices.size();
		final FloatBuffer vertexData = Buffers.newDirectFloatBuffer( 2 * nPoints );
		if ( ( xpVertex != null || xpEdge != null ) && ( ypVertex != null || ypEdge != null ) )
		{
			for ( final Spot v : vertices )
			{
				final float x = ( float ) getXFeatureValue( v );
				final float y = ( float ) getYFeatureValue( v );
				vertexData.put( x );
				vertexData.put( y );
			}
			vertexData.flip();
		}
		return vertexData;
	}

	private final double getXFeatureValue( final Spot v )
	{
		return getFeatureValue( v, xpVertex, xpEdge );
	}

	private final double getYFeatureValue( final Spot v )
	{
		return getFeatureValue( v, ypVertex, ypEdge );
	}

	private final double getFeatureValue( final Spot v, final FeatureProjection< Spot > xpv, final FeatureProjection< Link > xpe )
	{
		if ( xpv != null )
			return xpv.value( v );

		if ( xpe != null )
		{
			final Edges< Link > edges = ( incomingEdge )
					? v.incomingEdges()
					: v.outgoingEdges();
			if ( edges.size() != 1 )
				return Double.NaN;
			return xpe.value( edges.iterator().next() );
		}
		return Double.NaN;
	}
}
