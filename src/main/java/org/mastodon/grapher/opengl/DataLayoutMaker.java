package org.mastodon.grapher.opengl;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefIntMap;
import org.mastodon.collection.RefMaps;
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
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureSpecPair;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

public class DataLayoutMaker implements ContextListener< Spot >
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

	private RefSet< Spot > vertices;

	private String xlabel;

	private String ylabel;

	private Context< Spot > context;

	private final GraphColorGenerator< Spot, Link > graphColorGenerator;

	public DataLayoutMaker(
			final ModelGraph graph,
			final SelectionModel< Spot, Link > selection,
			final FeatureModel featureModel,
			final GraphColorGenerator< Spot, Link > graphColorGenerator )
	{
		this.graph = graph;
		this.selection = selection;
		this.featureModel = featureModel;
		this.graphColorGenerator = graphColorGenerator;
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
	public DataLayout layout()
	{
		if ( vertices.isEmpty() )
			return new DataLayout( new float[] {}, new int[] {} );

		/*
		 * Vertex pos.
		 */

		// Map of index in the pos array for each vertices.
		// Will be used later to build the edge index array.
		final RefIntMap< Spot > vPosMap = RefMaps.createRefIntMap( vertices, -1 );

		final int nPoints = vertices.size();
		final float[] xyPos = new float[ 2 * nPoints ];
		if ( ( xpVertex != null || xpEdge != null ) && ( ypVertex != null || ypEdge != null ) )
		{
			int i = 0;
			for ( final Spot v : vertices )
			{
				vPosMap.put( v, i / 2 );

				final float x = ( float ) getXFeatureValue( v );
				final float y = ( float ) getYFeatureValue( v );
				xyPos[ i++ ] = x;
				xyPos[ i++ ] = y;
			}
		}

		/*
		 * Edge indices.
		 */

		final int[] edgeIndices;
		if ( paintEdges )
		{
			final TIntHashSet visited = new TIntHashSet( vertices.size() );
			final TIntArrayList indexList = new TIntArrayList( vertices.size() * 2 );
			final Spot sref = vertices.createRef();
			final Spot tref = vertices.createRef();
			for ( final Spot v : vertices )
			{
				for ( final Link e : v.edges() )
				{
					final int id = e.getInternalPoolIndex();
					if ( visited.contains( id ) )
						continue;
					visited.add( id );

					indexList.add( vPosMap.get( e.getSource( sref ) ) );
					indexList.add( vPosMap.get( e.getTarget( tref ) ) );
				}
			}
			vertices.releaseRef( sref );
			vertices.releaseRef( tref );
			edgeIndices = indexList.toArray();
		}
		else
		{
			edgeIndices = new int[] {};
		}
		return new DataLayout( xyPos, edgeIndices );
	}

	/**
	 * Updates the color of the objects displayed based on the color generator
	 * specified at construction.
	 * 
	 * @return a <code>float[]</code> array, to be used by the OpenGL logic.
	 */
	public float[] color()
	{
		if ( vertices.isEmpty() )
			return new float[] {};

		final int n = vertices.size();
		final int COLOR_SIZE = 4;
		final float[] color = new float[ COLOR_SIZE * n ];

		int i = 0;
		for ( final Spot spot : vertices )
		{
			final int a;
			final int r;
			final int g;
			final int b;
			final int c = graphColorGenerator.color( spot );
			if ( c == 0 )
			{
				// Default cColor from the style. TODO
				a = 255;
				r = 0;
				g = 0;
				b = 0;
			}
			else
			{
				// Color from the colormap.
				a = ( c >> 24 ) & 0xFF;
				r = ( c >> 16 ) & 0xFF;
				g = ( c >> 8 ) & 0xFF;
				b = c & 255;
			}

			// RGBA
			color[ COLOR_SIZE * i + 0 ] = ( r / 255f );
			color[ COLOR_SIZE * i + 1 ] = ( g / 255f );
			color[ COLOR_SIZE * i + 2 ] = ( b / 255f );
			color[ COLOR_SIZE * i + 3 ] = ( a / 255f );
			i++;
		}
		return color;
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

	private void setVertices( final RefSet< Spot > vertices )
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

	public static final class DataLayout
	{
		public final float[] verticesPos;

		public final int[] edgeIndices;

		public DataLayout( final float[] verticesPos, final int[] edgeIndices )
		{
			this.verticesPos = verticesPos;
			this.edgeIndices = edgeIndices;
		}
	}
}
