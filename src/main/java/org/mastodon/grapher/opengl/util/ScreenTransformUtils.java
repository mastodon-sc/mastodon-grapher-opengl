package org.mastodon.grapher.opengl.util;

import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.DEFAULT_POINT_SIZE;

import org.mastodon.views.grapher.datagraph.ScreenTransform;

public class ScreenTransformUtils
{
	/**
	 * Gets the area in layout coordinates that corresponds to the screen area around (i.e. +/- {@link org.mastodon.grapher.opengl.overlays.DataPointsOverlay#DEFAULT_POINT_SIZE}/2) the specified screen coordinates.
	 * @param x screen x coordinate
	 * @param y screen y coordinate
	 * @param screenTransform the screen transform
	 * @return the area in layout coordinates
	 */
	public static double[] getDataPointArea(final double x, final double y, final ScreenTransform screenTransform )
	{
		return getDataPointArea( x, y, screenTransform, DEFAULT_POINT_SIZE );
	}

	/**
	 * Gets the area in layout coordinates that corresponds to the screen area around (i.e. +/- {@link org.mastodon.grapher.opengl.overlays.DataPointsOverlay#DEFAULT_POINT_SIZE}/2) the specified screen coordinates.
	 * @param x screen x coordinate
	 * @param y screen y coordinate
	 * @param screenTransform the screen transform
	 * @return the area in layout coordinates
	 */
	public static double[] getDataPointArea(final double x, final double y, final ScreenTransform screenTransform, float width )
	{
		float halfBboxSize = width / 2f;
		final double bboxXMin = screenTransform.screenToLayoutX( x + halfBboxSize );
		final double bboxYMin = screenTransform.screenToLayoutY( y + halfBboxSize );
		final double bboxXMax = screenTransform.screenToLayoutX( x - halfBboxSize );
		final double bboxYMax = screenTransform.screenToLayoutY( y - halfBboxSize );
		return new double[] { bboxXMin, bboxYMin, bboxXMax, bboxYMax };
	}
}
