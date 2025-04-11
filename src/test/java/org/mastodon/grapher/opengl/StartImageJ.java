package org.mastodon.grapher.opengl;

import java.lang.invoke.MethodHandles;

import org.scijava.Context;
import org.scijava.ui.UIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows the ImageJ main window, with Mastodon installed.
 *
 * @author Stefan Hahmann
 */
public class StartImageJ
{
	private static final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	public static void main( String... args )
	{
		logger.info( "Starting ImageJ..." );
		Context context = new Context();
		UIService uiService = context.service( UIService.class );
		uiService.showUI();
	}
}
