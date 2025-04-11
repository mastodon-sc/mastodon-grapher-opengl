package org.mastodon.grapher.opengl.util;

public class LineRectangleIntersection {

	// Check if a point is inside the rectangle
	public static boolean isPointInsideRectangle(float pointX, float pointY,
			float rectLeft, float rectBottom,
			float rectRight, float rectTop) {
		return pointX >= rectLeft && pointX <= rectRight &&
				pointY >= rectBottom && pointY <= rectTop;
	}

	// Check if two line segments intersect
	public static boolean doLineSegmentsIntersect(float line1StartX, float line1StartY,
			float line1EndX, float line1EndY,
			float line2StartX, float line2StartY,
			float line2EndX, float line2EndY) {
		float denominator = (line2EndY - line2StartY) * (line1EndX - line1StartX) -
				(line2EndX - line2StartX) * (line1EndY - line1StartY);

		if (denominator == 0) return false; // Parallel lines

		float line1IntersectionFactor = ((line2EndX - line2StartX) * (line1StartY - line2StartY) -
				(line2EndY - line2StartY) * (line1StartX - line2StartX)) / denominator;
		float line2IntersectionFactor = ((line1EndX - line1StartX) * (line1StartY - line2StartY) -
				(line1EndY - line1StartY) * (line1StartX - line2StartX)) / denominator;

		return (line1IntersectionFactor >= 0 && line1IntersectionFactor <= 1 &&
				line2IntersectionFactor >= 0 && line2IntersectionFactor <= 1);
	}

	// Check if a line segment intersects a rectangle
	public static boolean doesLineIntersectRectangle(float lineStartX, float lineStartY,
			float lineEndX, float lineEndY,
			float rectLeft, float rectBottom,
			float rectRight, float rectTop) {
		// Check if either endpoint is inside the rectangle
		if (isPointInsideRectangle(lineStartX, lineStartY, rectLeft, rectBottom, rectRight, rectTop) ||
				isPointInsideRectangle(lineEndX, lineEndY, rectLeft, rectBottom, rectRight, rectTop)) {
			return true;
		}

		// Check intersection with each edge of the rectangle
		boolean intersectsBottomEdge = doLineSegmentsIntersect(lineStartX, lineStartY, lineEndX, lineEndY,
				rectLeft, rectBottom, rectRight, rectBottom);
		boolean intersectsRightEdge = doLineSegmentsIntersect(lineStartX, lineStartY, lineEndX, lineEndY,
				rectRight, rectBottom, rectRight, rectTop);
		boolean intersectsTopEdge = doLineSegmentsIntersect(lineStartX, lineStartY, lineEndX, lineEndY,
				rectRight, rectTop, rectLeft, rectTop);
		boolean intersectsLeftEdge = doLineSegmentsIntersect(lineStartX, lineStartY, lineEndX, lineEndY,
				rectLeft, rectTop, rectLeft, rectBottom);

		return intersectsBottomEdge || intersectsRightEdge || intersectsTopEdge || intersectsLeftEdge;
	}

	public static void main(String[] args) {
		float lineStartX = 1, lineStartY = 1, lineEndX = 4, lineEndY = 4; // Line segment
		float rectLeft = 2, rectBottom = 2, rectRight = 5, rectTop = 5; // Rectangle bounds

		System.out.println("Does the line intersect the rectangle? " +
				doesLineIntersectRectangle(lineStartX, lineStartY, lineEndX, lineEndY, rectLeft, rectBottom, rectRight, rectTop));
	}
}
