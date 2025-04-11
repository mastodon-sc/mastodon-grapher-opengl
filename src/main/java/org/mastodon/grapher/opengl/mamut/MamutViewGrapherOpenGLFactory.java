/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.grapher.opengl.mamut;

import java.util.Map;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.views.AbstractMamutViewFactory;
import org.mastodon.mamut.views.MamutViewFactory;
import org.mastodon.mamut.views.grapher.GrapherGuiState;
import org.mastodon.mamut.views.grapher.MamutViewGrapher;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.ScreenTransformState;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Factory to Create and display OpenGL Grapher views.
 *
 * @see MamutViewGrapher
 */
@Plugin( type = MamutViewFactory.class, priority = Priority.NORMAL - 4 )
public class MamutViewGrapherOpenGLFactory extends AbstractMamutViewFactory< MamutViewGrapherOpenGL >
{

	public static final String NEW_GRAPHER_VIEW = "new grapher openGL view";


	@Override
	public MamutViewGrapherOpenGL create( final ProjectModel projectModel )
	{
		return new MamutViewGrapherOpenGL( projectModel );
	}

	@Override
	public Map< String, Object > getGuiState( final MamutViewGrapherOpenGL view )
	{
		final Map< String, Object > guiState = super.getGuiState( view );
		final ScreenTransformState screenTransformState = view.getFrame().getDataDisplayPanel().getScreenTransform();
		final FeatureGraphConfig config = view.getFrame().getVertexSidePanel().getGraphConfig();
		GrapherGuiState.writeGuiState( screenTransformState, config, guiState );
		return guiState;
	}


	@Override
	public void restoreGuiState( final MamutViewGrapherOpenGL view, final Map< String, Object > guiState )
	{
		super.restoreGuiState( view, guiState );

		GrapherGuiState.loadGuiState( guiState, view.getFrame().getDataDisplayPanel().getScreenTransform(), view.getFrame().getVertexSidePanel(),
				MamutViewGrapherOpenGL.getDefaultFeatureGraphConfig(), view.getFrame() );
	}

	@Override
	public String getCommandName()
	{
		return NEW_GRAPHER_VIEW;
	}

	@Override
	public String getCommandDescription()
	{
		return "Open a new OpenGL Grapher view.";
	}

	@Override
	public String getCommandMenuText()
	{
		return "New OpenGL Grapher";
	}

	@Override
	public Class< MamutViewGrapherOpenGL > getViewClass()
	{
		return MamutViewGrapherOpenGL.class;
	}
}
