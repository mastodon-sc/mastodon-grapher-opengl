package org.mastodon.grapher.opengl.overlays;

import org.lwjgl.opengl.awt.AWTGLCanvas;

/**
 * Draw something to an OpenGL canvas and receive notifications about changes of
 * the canvas size.
 */
public interface GLOverlayRenderer
{

	/**
	 * Prepare overlay. Called by the {@link AWTGLCanvas#initGL()} method.
	 */
	default void init()
	{}

	/**
	 * Render overlay. Called by the {@link AWTGLCanvas#paintGL()} method.
	 */
	void paint();

	/**
	 * This is called, when the screen size of the canvas (the component
	 * displaying the image and generating mouse events) changes. This can be
	 * used to determine scale of overlay or screen coordinates relative to the
	 * border.
	 *
	 * @param width
	 *            the new canvas width.
	 * @param height
	 *            the new canvas height.
	 */
	default void setCanvasSize( final int width, final int height )
	{}
}
