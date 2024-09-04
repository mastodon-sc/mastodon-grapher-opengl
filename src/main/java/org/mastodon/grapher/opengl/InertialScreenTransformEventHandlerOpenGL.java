package org.mastodon.grapher.opengl;


import org.mastodon.views.grapher.display.InertialScreenTransformEventHandler;
import org.mastodon.views.grapher.display.ScreenTransformState;

public class InertialScreenTransformEventHandlerOpenGL
		extends InertialScreenTransformEventHandler
{

	public InertialScreenTransformEventHandlerOpenGL( final ScreenTransformState transformState )
	{
		super( transformState );
	}

	public synchronized void layoutChanged( final float[] xy )
	{
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for ( int i = 0; i < xy.length; i++ )
		{
			final float x = xy[ i ];
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );
			i++;
			final float y = xy[ i ];
			minY = Math.min( minY, y );
			maxY = Math.max( maxY, y );
		}
		layoutChanged( minX, maxX, minY, maxY );
	}
}
