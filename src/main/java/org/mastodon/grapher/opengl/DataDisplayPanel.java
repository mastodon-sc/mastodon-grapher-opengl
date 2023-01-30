package org.mastodon.grapher.opengl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JPanel;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureProjection;
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

import bdv.viewer.render.PainterThread;

public class DataDisplayPanel extends JPanel implements MouseListener, MouseWheelListener, MouseMotionListener, ContextListener< Spot >
{

	private static final long serialVersionUID = 1L;

	private int[] vertexVBO;

	private int width, height;

	private int[] colorVBO;

	private double panX, panY;

	private boolean panning;

	private int lastX, lastY;

	private double zoom = 1;

	private FloatBuffer vertexData;

	private final DataLayout layout;

	private boolean trackContext = false;

	private final SelectionModel< Spot, Link > selection;

	private final ModelGraph graph;

	private Context< Spot > context;

	private Collection< Spot > vertices = Collections.emptyList();

	private FloatBuffer colorData;

	private boolean shouldRegenBuffer = false;

	private final FeatureModel featureModel;

	private final PainterThread painterThread;

	private final PointCloudCanvas canvas;

	public DataDisplayPanel(
			final DataLayout layout,
			final FeatureModel featureModel,
			final ModelGraph graph,
			final SelectionModel< Spot, Link > selection )
	{
		this.layout = layout;
		this.featureModel = featureModel;
		this.graph = graph;
		this.selection = selection;
		canvas = new PointCloudCanvas();

		canvas.addMouseListener( this );
		canvas.addMouseMotionListener( this );
		canvas.addMouseWheelListener( this );

		setPreferredSize( new Dimension( 400, 400 ) );
		setLayout( new BorderLayout() );
		add( canvas, BorderLayout.CENTER );

		painterThread = new PainterThread( () -> canvas.paintGL() );
		painterThread.start();
	}

	@Override
	protected void paintComponent( final Graphics g )
	{
		painterThread.requestRepaint();
	}

	public void plot( final FeatureGraphConfig gc )
	{
		layout.setPaintEdges( gc.drawConnected() );

		// X feature projection.
		final FeatureSpecPair spx = gc.getXFeature();
		final String xunits;
		if ( spx.isEdgeFeature() )
		{
			final FeatureProjection< Link > xproj = spx.getProjection( featureModel );
			layout.setXFeatureEdge( xproj, spx.isIncomingEdge() );
			xunits = xproj.units();
		}
		else
		{
			final FeatureProjection< Spot > xproj = spx.getProjection( featureModel );
			layout.setXFeatureVertex( xproj );
			xunits = xproj.units();
		}

		// Y feature projection.
		final String yunits;
		final FeatureSpecPair spy = gc.getYFeature();
		if ( spy.isEdgeFeature() )
		{
			final FeatureProjection< Link > yproj = spy.getProjection( featureModel );
			layout.setYFeatureEdge( yproj, spy.isIncomingEdge() );
			yunits = yproj.units();
		}
		else
		{
			final FeatureProjection< Spot > yproj = spy.getProjection( featureModel );
			layout.setYFeatureVertex( yproj );
			yunits = yproj.units();
		}

		switch ( gc.itemSource() )
		{
		case CONTEXT:
		{
			trackContext = true;
			vertices = fromContext();
			break;
		}
		case SELECTION:
		{
			vertices = selection.getSelectedVertices();
			break;
		}
		case TRACK_OF_SELECTION:
		{
			final RefSet< Spot > selectedVertices = selection.getSelectedVertices();
			final RefSet< Link > selectedEdges = selection.getSelectedEdges();
			vertices = fromTrackOfSelection( selectedVertices, selectedEdges );
			break;
		}
		case KEEP_CURRENT:
		default:
			break;
		}

		shouldRegenBuffer = true;
		painterThread.requestRepaint();
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		lastX = e.getX();
		lastY = e.getY();
		panning = true;
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		if ( panning )
		{
			panX += ( e.getX() - lastX ) / ( double ) width;
			panY -= ( e.getY() - lastY ) / ( double ) height;
			lastX = e.getX();
			lastY = e.getY();
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		panning = false;
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		zoom += e.getPreciseWheelRotation() * 0.1;
		zoom = Math.max( 0.1, Math.min( zoom, 10 ) );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{}

	@Override
	public void mouseClicked( final MouseEvent e )
	{}

	@Override
	public void mouseEntered( final MouseEvent e )
	{}

	@Override
	public void mouseExited( final MouseEvent e )
	{}

	@Override
	public void contextChanged( final Context< Spot > context )
	{
		if ( this.context == null && context == null )
			return;

		if ( trackContext )
			vertexData = layout.layout( fromContext() );

		this.context = context;
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
}
