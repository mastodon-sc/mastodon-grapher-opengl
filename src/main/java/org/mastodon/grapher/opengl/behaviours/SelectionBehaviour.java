package org.mastodon.grapher.opengl.behaviours;

public interface SelectionBehaviour
{
	void prepareSelection();
	void doSelection();
	void finishSelection();
	default void select()
	{
		prepareSelection();
		doSelection();
		finishSelection();
	}
}
