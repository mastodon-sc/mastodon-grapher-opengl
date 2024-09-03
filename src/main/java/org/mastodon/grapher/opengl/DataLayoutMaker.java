package org.mastodon.grapher.opengl;

import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.COLOR_SIZE;
import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.VERTEX_SIZE;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.graph.Edges;
import org.mastodon.graph.algorithm.traversal.DepthFirstSearch;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.graph.algorithm.traversal.SearchListener;
import org.mastodon.grapher.opengl.util.KdTreeWrapper;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.style.DataDisplayStyle;

import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.algorithm.kdtree.HyperPlane;

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

	private final HighlightModel< Spot, Link > highlight;

	private final SelectionModel< Spot, Link > selection;

	private RefSet< Spot > vertices;

	private RefSet< Link > edges;

	private String xlabel;

	private String ylabel;

	private Context< Spot > context;

	private final GraphColorGenerator< Spot, Link > graphColorGenerator;

	private KdTreeWrapper< Spot > kdtree;

	private final DataDisplayStyle style;

	public DataLayoutMaker(
			final ModelGraph graph,
			final HighlightModel< Spot, Link > highlight,
			final SelectionModel< Spot, Link > selection,
			final FeatureModel featureModel,
			final DataDisplayOptions options )
	{
		this.graph = graph;
		this.highlight = highlight;
		this.selection = selection;
		this.featureModel = featureModel;
		this.graphColorGenerator = options.values.getGraphColorGenerator();
		style = options.values.getStyle();
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
	 * Returns a new data layout containing the data points position and their
	 * links based on the current feature specifications for the current
	 * vertices in the data graph.
	 * 
	 * @return a new {@link DataLayout}.
	 */
	public DataLayout layout()
	{
		kdtree = null;
		if ( vertices.isEmpty() )
			return new DataLayout( new float[] {}, new int[] {}, new float[] {} );

		/*
		 * Vertex pos.
		 */

		final float[] xyPos = new float[ VERTEX_SIZE * vertices.size() ];
		if ( ( xpVertex != null || xpEdge != null ) && ( ypVertex != null || ypEdge != null ) )
		{
			int i = 0;
			for ( final Spot v : vertices )
			{
				final float x = ( float ) getXFeatureValue( v );
				final float y = ( float ) getYFeatureValue( v );
				xyPos[ i++ ] = x;
				xyPos[ i++ ] = y;
			}
			final List< ToDoubleFunction< Spot > > posFuns = new ArrayList<>( 2 );
			posFuns.add( v -> getXFeatureValue( v ) );
			posFuns.add( v -> getYFeatureValue( v ) );
			kdtree = new KdTreeWrapper<>( vertices, graph.vertices().getRefPool(), posFuns );
		}

		/*
		 * Edge indices.
		 */

		final int[] edgeIndices;
		final float[] edgePositions;
		if ( paintEdges )
		{
			edgeIndices = new int[ edges.size() * 2 ];
			edgePositions = new float[ edges.size() * 2 * VERTEX_SIZE ];

			final Spot sref = vertices.createRef();
			final Spot tref = vertices.createRef();
			int ii = 0;
			int ip = 0;
			for ( final Link e : edges )
			{
				final Spot source = e.getSource( sref );
				final Spot target = e.getTarget( tref );
				final float xs = ( float ) getXFeatureValue( source );
				final float ys = ( float ) getYFeatureValue( source );
				final float xt = ( float ) getXFeatureValue( target );
				final float yt = ( float ) getYFeatureValue( target );
				edgePositions[ ip++ ] = xs;
				edgePositions[ ip++ ] = ys;
				edgePositions[ ip++ ] = xt;
				edgePositions[ ip++ ] = yt;
				edgeIndices[ ii ] = ii++;
				edgeIndices[ ii ] = ii++;
			}
			vertices.releaseRef( sref );
			vertices.releaseRef( tref );
		}
		else
		{
			edgeIndices = new int[] {};
			edgePositions = new float[] {};
		}
		return new DataLayout( xyPos, edgeIndices, edgePositions );
	}

	public float[][] getHighlightVertexData()
	{
		final Spot vref = graph.vertexRef();
		try
		{
			final Spot hv = highlight.getHighlightedVertex( vref );
			if ( hv == null )
				return null;

			final float[] color = new float[ 4 ];
			colorVertex( hv, color );

			final Color bg = style.getBackgroundColor();
			return new float[][] {
					new float[] {
							( float ) getXFeatureValue( hv ),
							( float ) getYFeatureValue( hv ) },
					color,
					new float[] {
							bg.getRed() / 255f,
							bg.getGreen() / 255f,
							bg.getBlue() / 255f,
							bg.getAlpha() / 255f }
			};
		}
		finally
		{
			graph.releaseRef( vref );
		}
	}

	/**
	 * Returns a new color specification for the objects displayed based on the
	 * color generator specified at construction.
	 * 
	 * @return a new {@link DataColor}, to be used by the OpenGL logic.
	 */
	public DataColor color()
	{
		if ( vertices.isEmpty() )
			return new DataColor( new float[] {}, new float[] {} );

		/*
		 * Vertex colors.
		 */

		final int n = vertices.size();
		final float[] vertexColors = new float[ COLOR_SIZE * n ];
		final float[] tmp = new float[ 4 ];
		int i = 0;
		for ( final Spot spot : vertices )
		{
			// RGBA
			colorVertex( spot, tmp );
			vertexColors[ i++ ] = tmp[ 0 ];
			vertexColors[ i++ ] = tmp[ 1 ];
			vertexColors[ i++ ] = tmp[ 2 ];
			vertexColors[ i++ ] = tmp[ 3 ];
		}

		/*
		 * Edge colors.
		 */

		final Color eColor = style.getEdgeColor();
		final int eRed = eColor.getRed();
		final int eGreen = eColor.getGreen();
		final int eBlue = eColor.getBlue();
		final int eAlpha = eColor.getAlpha();

		final Color seColor = style.getSelectedEdgeColor();
		final int seRed = seColor.getRed();
		final int seGreen = seColor.getGreen();
		final int seBlue = seColor.getBlue();
		final int seAlpha = seColor.getAlpha();

		final float[] edgeColors;
		int j = 0;
		if ( paintEdges )
		{
			edgeColors = new float[ edges.size() * COLOR_SIZE * 2 ];
			final Spot sref = vertices.createRef();
			final Spot tref = vertices.createRef();
			for ( final Link e : edges )
			{
				e.getSource( sref );
				e.getTarget( tref );

				final int a;
				final int r;
				final int g;
				final int b;
				if ( selection.isSelected( e ) )
				{
					a = seAlpha;
					r = seRed;
					g = seGreen;
					b = seBlue;
				}
				else
				{
					final int c = graphColorGenerator.color( e, sref, tref );
					if ( c == 0 )
					{
						// Default cColor from the style.
						a = eAlpha;
						r = eRed;
						g = eGreen;
						b = eBlue;
					}
					else
					{
						// Color from the colormap.
						a = ( c >> 24 ) & 0xFF;
						r = ( c >> 16 ) & 0xFF;
						g = ( c >> 8 ) & 0xFF;
						b = c & 255;
					}
				}

				// RGBA
				edgeColors[ j++ ] = r / 255f;
				edgeColors[ j++ ] = g / 255f;
				edgeColors[ j++ ] = b / 255f;
				edgeColors[ j++ ] = a / 255f;
				edgeColors[ j++ ] = r / 255f;
				edgeColors[ j++ ] = g / 255f;
				edgeColors[ j++ ] = b / 255f;
				edgeColors[ j++ ] = a / 255f;
			}
			vertices.releaseRef( sref );
			vertices.releaseRef( tref );
		}
		else
		{
			edgeColors = new float[] {};
		}

		return new DataColor( vertexColors, edgeColors );
	}

	private void colorVertex( final Spot spot, final float[] out )
	{
		final Color vColor = style.getSimplifiedVertexFillColor();
		final int vRed = vColor.getRed();
		final int vGreen = vColor.getGreen();
		final int vBlue = vColor.getBlue();
		final int vAlpha = vColor.getAlpha();

		final Color svColor = style.getSelectedSimplifiedVertexFillColor();
		final int svRed = svColor.getRed();
		final int svGreen = svColor.getGreen();
		final int svBlue = svColor.getBlue();
		final int svAlpha = svColor.getAlpha();

		final int a;
		final int r;
		final int g;
		final int b;
		if ( selection.isSelected( spot ) )
		{
			r = svRed;
			g = svGreen;
			b = svBlue;
			a = svAlpha;
		}
		else
		{
			final int c = graphColorGenerator.color( spot );
			if ( c == 0 )
			{
				a = vAlpha;
				r = vRed;
				g = vGreen;
				b = vBlue;
			}
			else
			{
				// Color from the colormap.
				a = ( c >> 24 ) & 0xFF;
				r = ( c >> 16 ) & 0xFF;
				g = ( c >> 8 ) & 0xFF;
				b = c & 255;
			}
		}
		out[ 0 ] = ( r / 255f );
		out[ 1 ] = ( g / 255f );
		out[ 2 ] = ( b / 255f );
		out[ 3 ] = ( a / 255f );
	}

	private void setVertices( final RefSet< Spot > vertices )
	{
		this.vertices = vertices;
		this.edges = RefCollections.createRefSet( graph.edges(), vertices.size() );
		final Spot sref = vertices.createRef();
		final Spot tref = vertices.createRef();
		for ( final Spot v : vertices )
		{
			for ( final Link e : v.edges() )
			{
				final Spot source = e.getSource( sref );
				final Spot target = e.getTarget( tref );
				if ( vertices.contains( source ) && vertices.contains( target ) )
					edges.add( e );
			}
		}
		vertices.releaseRef( sref );
		vertices.releaseRef( tref );
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

	/**
	 * Returns the set of data vertices that are painted according to this
	 * layout instance, within the specified <b>screen coordinates</b>.
	 * 
	 * @param x1
	 *            x min in screen coordinates.
	 * @param y1
	 *            y min in screen coordinates.
	 * @param x2
	 *            x max in screen coordinates.
	 * @param y2
	 *            y max in screen coordinates.
	 * @return a new {@link RefSet}.
	 */
	public RefSet< Spot > getSpotWithin( final double x1, final double y1, final double x2, final double y2 )
	{
		final RefSet< Spot > set = RefCollections.createRefSet( graph.vertices() );
		if ( kdtree == null )
			return set;

		final double lx1 = Math.min( x1, x2 );
		final double lx2 = Math.max( x1, x2 );
		final double ly1 = Math.min( y1, y2 );
		final double ly2 = Math.max( y1, y2 );

		// Make hyperplanes for transform view.
		final HyperPlane hpMinX = new HyperPlane( new double[] { 1., 0. }, lx1 );
		final HyperPlane hpMaxX = new HyperPlane( new double[] { -1., 0. }, -lx2 );
		final HyperPlane hpMinY = new HyperPlane( new double[] { 0., 1. }, ly1 );
		final HyperPlane hpMaxY = new HyperPlane( new double[] { 0., -1. }, -ly2 );

		// Convex polytope from hyperplanes.
		final ConvexPolytope polytope = new ConvexPolytope( hpMinX, hpMinY, hpMaxX, hpMaxY );

		return kdtree.getObjsWithin( polytope );
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

		public final float[] edgePositions;

		public DataLayout(
				final float[] verticesPos,
				final int[] edgeIndices,
				final float[] edgePositions )
		{
			this.verticesPos = verticesPos;
			this.edgeIndices = edgeIndices;
			this.edgePositions = edgePositions;
		}
	}

	public static final class DataColor
	{
		public final float[] verticesColor;

		public final float[] edgesColor;

		public DataColor( final float[] verticesColor, final float[] edgesColor )
		{
			this.verticesColor = verticesColor;
			this.edgesColor = edgesColor;
		}
	}
}
