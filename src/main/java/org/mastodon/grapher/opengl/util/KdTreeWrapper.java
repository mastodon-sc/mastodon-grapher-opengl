package org.mastodon.grapher.opengl.util;

import java.util.Collection;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.mastodon.RefPool;
import org.mastodon.adapter.CollectionAdapter;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.grapher.opengl.util.RealLocalizableWrapperBiMap.RL;
import org.mastodon.kdtree.ClipConvexPolytopeKDTree;
import org.mastodon.kdtree.KDTree;
import org.mastodon.pool.DoubleMappedElement;

import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.kdtree.ConvexPolytope;

/**
 * Wraps a collection of {@link RefPool} objects so that they can be used for
 * Kd-tree searches, specifying their coordinates with user-specified functions.
 * 
 * @param <O>
 *            the type of objects to wrap. Does not have to be
 *            {@link RealLocalizable}.
 */
public class KdTreeWrapper< O >
{

	private final KDTree< RL< O >, DoubleMappedElement > tree;

	private final RefPool< O > pool;

	/**
	 * Creates a new KD-tree wrapper.
	 * 
	 * @param objs
	 *            the collection of objects to perform KD-tree searches on.
	 * @param pool
	 *            the {@link RefPool} of these objects.
	 * @param posFuns
	 *            the list of functions that map the object to a spatial
	 *            coordinate. First item in the list is the first coordinate,
	 *            etc. To create 3D objects, give 3 functions in the list, etc.
	 */
	public KdTreeWrapper(
			final RefCollection< O > objs,
			final RefPool< O > pool,
			final List< ToDoubleFunction< O > > posFuns )
	{
		this.pool = pool;
		final RealLocalizableWrapperBiMap< O > map = new RealLocalizableWrapperBiMap<>( pool, posFuns );
		final Collection< RL< O > > set = new CollectionAdapter<>( objs, map );
		this.tree = KDTree.kdtree( set, map );
	}

	public RefSet< O > getObjsWithin( final ConvexPolytope polytope )
	{
		final ClipConvexPolytopeKDTree< RL< O >, DoubleMappedElement > clip = new ClipConvexPolytopeKDTree<>( tree );
		clip.clip( polytope );
		final RefSet< O > set = new RefSetImp<>( pool );
		for ( final RL< O > obj : clip.getInsideValues() )
			set.add( obj.wv );

		return set;
	}
}
