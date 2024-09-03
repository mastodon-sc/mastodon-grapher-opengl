package org.mastodon.grapher.opengl.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.junit.Test;
import org.mastodon.adapter.CollectionAdapter;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.grapher.opengl.util.RealLocalizableWrapperBiMap.RL;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

public class RealLocalizableWrapperBiMapTest
{

	@Test
	public void testCoordinateMapping()
	{
		// Create a small model.
		final ModelGraph graph = new ModelGraph( 10 );
		final Spot refa = graph.vertexRef();
		final int n = 10;
		final RefList< Spot > list1 = RefCollections.createRefList( graph.vertices(), n );
		try
		{
			for ( int i = 0; i < n; i++ )
			{
				// time-point will be the index in the list.
				final int t = i;
				final double x = i;
				final double y = 10. - i;
				final double z = 3. * i;
				final Spot v = graph.addVertex( refa ).init( t, new double[] { x, y, z }, 1. );
				list1.add( v );
			}
		}
		finally
		{
			graph.releaseRef( refa );
		}
		
		// Map vertices to a new collection where x = time-point and y = x.
		final List< ToDoubleFunction< Spot > > posFuns = Arrays.asList(
				v -> v.getTimepoint(),
				v -> v.getDoublePosition( 0 ) );
		final RealLocalizableWrapperBiMap< Spot > map = new RealLocalizableWrapperBiMap<>( graph.vertices().getRefPool(), posFuns );
		final Collection< RL< Spot > > list2 = new CollectionAdapter<>( graph.vertices(), map );

		final Spot refb = graph.vertexRef();
		try
		{
			for ( final RL< Spot > rl : list2 )
			{
				final int t = ( int ) rl.getDoublePosition( 0 );
				final double y2 = rl.getDoublePosition( 1 );

				final Spot v1 = list1.get( t, refb );
				final double x1 = v1.getDoublePosition( 0 );

				assertEquals( "Y coordinates of the wrapper object does not match the X coordinate of the wrapped object.", x1, y2, Double.MIN_VALUE );
			}
		}
		finally
		{
			graph.releaseRef( refb );
		}
	}
}
