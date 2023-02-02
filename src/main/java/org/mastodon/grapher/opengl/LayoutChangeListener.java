package org.mastodon.grapher.opengl;

/**
 * Interface for classes that get notified when the layout of the OpenGL canvas
 * is changed.
 * 
 * @author Jean-Yves Tinevez
 *
 */
public interface LayoutChangeListener
{
	void layoutChanged( float minX, float maxX, float minY, float maxY );

}
