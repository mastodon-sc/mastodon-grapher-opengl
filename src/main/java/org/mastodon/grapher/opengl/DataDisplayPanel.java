package org.mastodon.grapher.opengl;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;

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
import org.mastodon.ui.coloring.ColorMap;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureSpecPair;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import bdv.viewer.render.PainterThread;

public class DataDisplayPanel extends JPanel implements GLEventListener, MouseListener, MouseWheelListener, MouseMotionListener, ContextListener< Spot >
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

	private final GLCanvas canvas;

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
		final GLProfile profile = GLProfile.getDefault();
		final GLCapabilities capabilities = new GLCapabilities( profile );
		canvas = new GLCanvas( capabilities );

		canvas.addMouseListener( this );
		canvas.addMouseMotionListener( this );
		canvas.addMouseWheelListener( this );
		canvas.addGLEventListener( this );

		setPreferredSize( new Dimension( 400, 400 ) );
		setLayout( new BorderLayout() );
		add( canvas, BorderLayout.CENTER );

		painterThread = new PainterThread( () -> canvas.display() );
		painterThread.start();
	}

	@Override
	protected void paintComponent( final Graphics g )
	{
		painterThread.requestRepaint();
	}

	@Override
	public void init( final GLAutoDrawable drawable )
	{

		// GL class.
		final GL2 gl = drawable.getGL().getGL2();

		if ( vertexVBO != null )
		{
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vertexVBO[ 0 ] );
			gl.glDeleteBuffers( 1, vertexVBO, 0 );
		}
		if ( colorVBO != null )
		{
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, colorVBO[ 0 ] );
			gl.glDeleteBuffers( 1, colorVBO, 0 );
		}

		if ( vertices.isEmpty() )
		{
			// Generate random vertex data
			final int nPoints = 100;
			vertexData = Buffers.newDirectFloatBuffer( 2 * nPoints );
			colorData = FloatBuffer.allocate( 3 * nPoints );

			final ColorMap cm = ColorMap.getColorMap( ColorMap.JET.getName() );
			for ( int i = 0; i < nPoints; i++ )
			{
//			final double x = ran.nextDouble() * 1.8 - 0.9;
//			final double y = ran.nextDouble() * 1.8 - 0.9;
				final float x = ( float ) i / nPoints * 2 - 1;
				final float y = ( float ) Math.cos( 2. * x * Math.PI );
				vertexData.put( x );
				vertexData.put( y );
				final int c = cm.get( ( float ) i / nPoints );
//			final int a = ( c >> 24 ) & 0xFF;
				final int r = ( c >> 16 ) & 0xFF;
				final int g = ( c >> 8 ) & 0xFF;
				final int b = c & 255;
				colorData.put( r / 255f );
				colorData.put( g / 255f );
				colorData.put( b / 255f );
			}
			vertexData.flip();
			colorData.flip();
		}
		else
		{
			final int nPoints = vertices.size();
			System.out.println( "Recreating buffers for " + nPoints + " points." ); // DEBUG
			vertexData = layout.layout( vertices );

			colorData = FloatBuffer.allocate( 3 * nPoints );
			final ColorMap cm = ColorMap.getColorMap( ColorMap.JET.getName() );
			for ( int i = 0; i < nPoints; i++ )
			{
				final int c = cm.get( ( float ) i / nPoints );
//			final int a = ( c >> 24 ) & 0xFF;
				final int r = ( c >> 16 ) & 0xFF;
				final int g = ( c >> 8 ) & 0xFF;
				final int b = c & 255;
				colorData.put( r / 255f );
				colorData.put( g / 255f );
				colorData.put( b / 255f );
			}
			colorData.flip();
		}

		// Reset view.
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for ( int i = 0; i < vertexData.limit(); i++ )
		{
			final float x = vertexData.get( i );
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );
			i++;
			final float y = vertexData.get( i );
			minY = Math.min( minY, y );
			maxY = Math.max( maxY, y );
		}

		System.out.println( "minX = " + minX + "  maxX = " + maxX + "  minY = " + minY + "  maxY = " + maxY ); // DEBUG

		gl.glMatrixMode( GL2.GL_PROJECTION );
		gl.glLoadIdentity();
		gl.glOrtho( minX, maxX, minY, maxY, -1, 1 );
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();

		// Vertex buffer -> 2D double.
		vertexVBO = new int[ 1 ];
		gl.glGenBuffers( 1, vertexVBO, 0 );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vertexVBO[ 0 ] );
		gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
		gl.glVertexPointer( 2, GL2.GL_FLOAT, 0, 0 );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, vertexData.capacity() * Float.BYTES,
				vertexData, GL2.GL_STATIC_DRAW );

		// Color buffer -> 3D float.
		colorVBO = new int[ 1 ];
		gl.glGenBuffers( 1, colorVBO, 0 );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, colorVBO[ 0 ] );
		gl.glEnableClientState( GL2.GL_COLOR_ARRAY );
		gl.glColorPointer( 3, GL_FLOAT, 0, 0l );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, colorData.capacity() * Float.BYTES,
				colorData, GL.GL_STATIC_DRAW );
	}

	@Override
	public void display( final GLAutoDrawable drawable )
	{
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );

//		gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
		gl.glPointSize( 5f );

		if ( shouldRegenBuffer )
		{
			init( drawable );
			shouldRegenBuffer = false;
		}


		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vertexVBO[ 0 ] );

		final int nPoints = vertexData.capacity() / 2;

		// Handle panning.
//		gl.glMatrixMode( GL2.GL_MODELVIEW );
//		gl.glLoadIdentity();
//		gl.glTranslated( panX, panY, 0 );
//		gl.glScaled( zoom, zoom, 1 );
		gl.glDrawArrays( GL2.GL_POINTS, 0, nPoints );
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
	public void reshape( final GLAutoDrawable drawable, final int x, final int y, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		final GL2 gl = drawable.getGL().getGL2();
		gl.glViewport( 0, 0, width, height );
	}

	@Override
	public void dispose( final GLAutoDrawable drawable )
	{
		painterThread.interrupt();
		final GL2 gl = drawable.getGL().getGL2();
		gl.glDeleteBuffers( 1, vertexVBO, 0 );
		gl.glDeleteBuffers( 1, colorVBO, 0 );
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
