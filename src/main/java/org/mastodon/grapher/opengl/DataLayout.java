package org.mastodon.grapher.opengl;

import java.util.Collection;
import java.util.Collections;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.graph.Edges;
import org.mastodon.graph.algorithm.traversal.DepthFirstSearch;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.graph.algorithm.traversal.SearchListener;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureSpecPair;

public class DataLayout implements ContextListener< Spot >
{

	private FeatureProjection< Spot > ypVertex;

	private FeatureProjection< Spot > xpVertex;

	private FeatureProjection< Link > xpEdge;

	private FeatureProjection< Link > ypEdge;

	private boolean incomingEdge;

	private boolean paintEdges;

	private boolean trackContext;

	private final FeatureModel featureModel;

	private final ModelGraph graph;

	private final SelectionModel< Spot, Link > selection;

	private Collection< Spot > vertices = Collections.emptyList();

	private String xlabel;

	private String ylabel;

	private Context< Spot > context;

	public DataLayout( final ModelGraph graph, final SelectionModel< Spot, Link > selection, final FeatureModel featureModel )
	{
		this.graph = graph;
		this.selection = selection;
		this.featureModel = featureModel;
	}

	private void setXFeatureVertex( final FeatureProjection< Spot > xproj )
	{
		this.xpVertex = xproj;
		this.xpEdge = null;
	}

	private void setYFeatureVertex( final FeatureProjection< Spot > yproj )
	{
		this.ypVertex = yproj;
		this.ypEdge = null;
	}

	private void setXFeatureEdge( final FeatureProjection< Link > xproj, final boolean incoming )
	{
		this.xpEdge = xproj;
		this.incomingEdge = incoming;
		this.xpVertex = null;
	}

	private void setYFeatureEdge( final FeatureProjection< Link > yproj, final boolean incoming )
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
	private void setPaintEdges( final boolean paintEdges )
	{
		this.paintEdges = paintEdges;
	}

	/**
	 * Resets X and Y position based on the current feature specifications for
	 * the current vertices in the data graph.
	 * 
	 * @return
	 */
	public float[] layout()
	{
		if ( vertices.isEmpty() )
		{
			return new float[] {};
		}

		final int nPoints = vertices.size();
		final float[] out = new float[ 2 * nPoints ];
		if ( ( xpVertex != null || xpEdge != null ) && ( ypVertex != null || ypEdge != null ) )
		{
			int i = 0;
			for ( final Spot v : vertices )
			{
				final float x = ( float ) getXFeatureValue( v );
				final float y = ( float ) getYFeatureValue( v );
				out[ i++ ] = x;
				out[ i++ ] = y;
			}
		}
		return out;
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

	public void setConfig( final FeatureGraphConfig gc )
	{
		trackContext = false;

		// X feature projection.
		final FeatureSpecPair spx = gc.getXFeature();
		final String xunits;
		if ( spx.isEdgeFeature() )
		{
			final FeatureProjection< Link > xproj = spx.getProjection( featureModel );
			setXFeatureEdge( xproj, spx.isIncomingEdge() );
			xunits = xproj.units();
		}
		else
		{
			final FeatureProjection< Spot > xproj = spx.getProjection( featureModel );
			setXFeatureVertex( xproj );
			xunits = xproj.units();
		}

		// Y feature projection.
		final String yunits;
		final FeatureSpecPair spy = gc.getYFeature();
		if ( spy.isEdgeFeature() )
		{
			final FeatureProjection< Link > yproj = spy.getProjection( featureModel );
			setYFeatureEdge( yproj, spy.isIncomingEdge() );
			yunits = yproj.units();
		}
		else
		{
			final FeatureProjection< Spot > yproj = spy.getProjection( featureModel );
			setYFeatureVertex( yproj );
			yunits = yproj.units();
		}

		// Vertices to plot.
		final RefSet< Spot > selectedVertices = selection.getSelectedVertices();
		final RefSet< Link > selectedEdges = selection.getSelectedEdges();
		switch ( gc.itemSource() )
		{
		case CONTEXT:
		{
			trackContext = true;
			setVertices( fromContext() );
			break;
		}
		case SELECTION:
		{
			setVertices( selection.getSelectedVertices() );
			break;
		}
		case TRACK_OF_SELECTION:
		{
			final RefSet< Spot > vertices = fromTrackOfSelection( selectedVertices, selectedEdges );
			setVertices( vertices );
			break;
		}
		case KEEP_CURRENT:
		default:
			break;
		}

		// Draw plot edges.
		setPaintEdges( gc.drawConnected() );

		String xlabel = gc.getXFeature().toString();
		if ( !xunits.isEmpty() )
			xlabel += " (" + xunits + ")";
		this.xlabel = xlabel;

		String ylabel = gc.getYFeature().toString();
		if ( !yunits.isEmpty() )
			ylabel += " (" + yunits + ")";
		this.ylabel = ylabel;
	}

	public String getXLabel()
	{
		return xlabel;
	}

	public String getYLabel()
	{
		return ylabel;
	}

	private void setVertices( final Collection< Spot > vertices )
	{
		this.vertices = vertices;
	}

	private RefSet< Spot > fromContext()
	{
		final Iterable< Spot > iterable;
		if ( context != null )
		{
			iterable = context.getInsideVertices( context.getTimepoint() );
		}
		else
			iterable = graph.vertices();

		final RefSet< Spot > vertices = RefCollections.createRefSet( graph.vertices() );
		for ( final Spot v : iterable )
			vertices.add( v );
		return vertices;
	}

	private RefSet< Spot > fromTrackOfSelection(
			final RefSet< Spot > selectedVertices,
			final RefSet< Link > selectedEdges )
	{
		final RefSet< Spot > toSearch = RefCollections.createRefSet( graph.vertices() );
		toSearch.addAll( selectedVertices );
		final Spot ref = graph.vertexRef();
		for ( final Link e : selectedEdges )
		{
			toSearch.add( e.getSource( ref ) );
			toSearch.add( e.getTarget( ref ) );
		}
		graph.releaseRef( ref );

		// Prepare the iterator.
		final RefSet< Spot > set = RefCollections.createRefSet( graph.vertices() );
		final DepthFirstSearch< Spot, Link > search =
				new DepthFirstSearch<>( graph, SearchDirection.UNDIRECTED );
		search.setTraversalListener(
				new SearchListener< Spot, Link, DepthFirstSearch< Spot, Link > >()
				{
					@Override
					public void processVertexLate( final Spot vertex,
							final DepthFirstSearch< Spot, Link > search )
					{}

					@Override
					public void processVertexEarly( final Spot vertex,
							final DepthFirstSearch< Spot, Link > search )
					{
						set.add( vertex );
					}

					@Override
					public void processEdge( final Link edge, final Spot from, final Spot to,
							final DepthFirstSearch< Spot, Link > search )
					{}

					@Override
					public void crossComponent( final Spot from, final Spot to,
							final DepthFirstSearch< Spot, Link > search )
					{}
				} );

		for ( final Spot v : toSearch )
			if ( !set.contains( v ) )
				search.start( v );
		return set;
	}

	@Override
	public void contextChanged( final Context< Spot > context )
	{
		this.context = context;
	}
}
