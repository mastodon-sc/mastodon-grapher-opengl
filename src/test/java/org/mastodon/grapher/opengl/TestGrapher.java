package org.mastodon.grapher.opengl;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.mastodon.mamut.launcher.MastodonLauncherCommand;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.ui.UIService;

import mpicbg.spim.data.SpimDataException;

public class TestGrapher
{

	public static void main( final String[] args ) throws IOException, SpimDataException, InterruptedException, ExecutionException
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		@SuppressWarnings( "resource" )
		final Context context = new Context();
		final UIService uiService = context.service( UIService.class );
		uiService.showUI();
		final CommandService commandService = context.service( CommandService.class );
		commandService.run( MastodonLauncherCommand.class, true );
	}
}
