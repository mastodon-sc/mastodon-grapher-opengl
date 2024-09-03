package org.mastodon.grapher.opengl.util;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.ToDoubleFunction;

import org.mastodon.RefPool;
import org.mastodon.adapter.RefBimap;
import org.mastodon.grapher.opengl.util.RealLocalizableWrapperBiMap.RL;
import org.mastodon.kdtree.KDTree;

import net.imglib2.RealLocalizable;

/**
 * A bi-directional map that wraps {@link RefPool} objects into
 * {@link RealLocalizable}s.
 * <p>
 * The bimap is to be used to create collections or Kd-trees of
 * {@link RL}s that map the input object into a wrapper
 * {@link RealLocalizable} object. The position of the wrapper objects are given
 * be a list of function that return a <code>double</code>, one for each
 * coordinate of the wrapper object.
 * <p>
 * For instance, you can provide a list of objects that are not
 * {@link RealLocalizable}, create functions that read their x and y positions
 * from two arrays, and use this class to create a collection of
 * {@link RealLocalizable}s with these arrays as coordinates.
 * <p>
 * This was initially created to use the Mastodon {@link KDTree} classes with
 * objects for which the coordinates are specified by configurable, external
 * feature values.
 * 
 * @param <V>
 *            the type of objects to wrap.
 */
public class RealLocalizableWrapperBiMap< V > implements RefBimap< V, RL< V > >, RefPool< RL< V > >
{

	private final ConcurrentLinkedQueue< RL< V > > tmpVertexRefs;

	private final List< ToDoubleFunction< V > > posFuns;

	private final RefPool< V > pool;

	public RealLocalizableWrapperBiMap(
			final RefPool< V > pool,
			final List< ToDoubleFunction< V > > posFuns )
	{
		this.pool = pool;
		this.posFuns = posFuns;
		this.tmpVertexRefs = new ConcurrentLinkedQueue<>();
	}

	/*
	 * RefBiMap methods.
	 */

	@Override
	public RL< V > getRight( final V left, final RL< V > ref )
	{
		ref.wv = left;
		return ref.orNull();
	}

	@Override
	public V reusableLeftRef( final RL< V > right )
	{
		return right.ref;
	}

	@Override
	public RL< V > reusableRightRef()
	{
		final RL< V > ref = tmpVertexRefs.poll();
		return ref == null ? new RL<>( pool, posFuns ) : ref;
	}

	@Override
	public void releaseRef( final RL< V > ref )
	{
		tmpVertexRefs.add( ref );
	}

	@Override
	public V getLeft( final RL< V > right )
	{
		return right == null ? null : right.wv;
	}

	/*
	 * RefPool methods.
	 */

	@Override
	public RL< V > createRef()
	{
		return reusableRightRef();
	}

	@Override
	public RL< V > getObject( final int id, final RL< V > obj )
	{
		obj.wv = pool.getObject( id, obj.ref );
		return obj;
	}

	@Override
	public RL< V > getObjectIfExists( final int id, final RL< V > obj )
	{
		obj.wv = pool.getObjectIfExists( id, obj.ref );
		return obj.wv == null ? null : obj;
	}

	@Override
	public int getId( final RL< V > obj )
	{
		return pool.getId( obj.wv );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public Class< RL< V > > getRefClass()
	{
		return ( Class ) RL.class;
	}

	public static class RL< V > implements RealLocalizable
	{

		V wv;
		
		final V ref;

		private final List< ToDoubleFunction< V > > posFuns;

		private RL( final RefPool< V > wrapped, final List< ToDoubleFunction< V > > posFuns )
		{
			this.posFuns = posFuns;
			ref = wrapped.createRef();
		}

		@Override
		public int hashCode()
		{
			return wv.hashCode();
		}

		@Override
		public boolean equals( final Object obj )
		{
			return obj instanceof RealLocalizableWrapperBiMap.RL &&
					wv.equals( ( ( RL< ? > ) obj ).wv );
		}

		RL< V > orNull()
		{
			return wv == null ? null : this;
		}

		@Override
		public int numDimensions()
		{
			return posFuns.size();
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return posFuns.get( d ).applyAsDouble( wv );
		}
	}
}
