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

import static org.mastodon.mamut.views.grapher.MamutViewGrapherFactory.GRAPHER_TRANSFORM_KEY;

import java.util.Map;

import org.mastodon.grapher.opengl.MamutViewOpenGL;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.views.AbstractMamutViewFactory;
import org.mastodon.mamut.views.MamutViewFactory;
import org.mastodon.mamut.views.grapher.MamutViewGrapher;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Factory to Create and display OpenGL Grapher views.
 *
 * @see MamutViewGrapher
 */
@Plugin( type = MamutViewFactory.class, priority = Priority.NORMAL - 4 )
public class MamutViewGrapherOpenGLFactory extends AbstractMamutViewFactory< MamutViewOpenGL >
{

	public static final String NEW_GRAPHER_VIEW = "new grapher openGL view";


	@Override
	public MamutViewOpenGL create( final ProjectModel projectModel )
	{
		return new MamutViewOpenGL( projectModel );
	}

	@Override
	public Map< String, Object > getGuiState( final MamutViewOpenGL view )
	{
		final Map< String, Object > guiState = super.getGuiState( view );
		// Transform.
		final ScreenTransform t = view.getFrame().getDataDisplayPanel().getScreenTransform().get();
		guiState.put( GRAPHER_TRANSFORM_KEY, t );

		return guiState;
	}


	@Override
	public void restoreGuiState( final MamutViewOpenGL view, final Map< String, Object > guiState )
	{
		super.restoreGuiState( view, guiState );

		// Transform.
		final ScreenTransform tLoaded = ( ScreenTransform ) guiState.get( GRAPHER_TRANSFORM_KEY );
		if ( null != tLoaded )
			view.getFrame().getDataDisplayPanel().getScreenTransform().set( tLoaded );
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
	public Class< MamutViewOpenGL > getViewClass()
	{
		return MamutViewOpenGL.class;
	}
}
