/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.grapher.opengl;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Element;
import org.mastodon.app.ui.MastodonFrameView;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.branch.BranchLink;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.ColoringModelMain;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.context.ContextProvider;
import org.mastodon.views.grapher.display.DataDisplayPanel;
import org.mastodon.views.trackscheme.ScreenTransform;
import org.mastodon.views.trackscheme.display.ColorBarOverlay;
import org.mastodon.views.trackscheme.display.ColorBarOverlay.Position;

import bdv.viewer.ViewerState;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Collection of constants and utilities related to de/serializing a GUI state.
 */
class MamutViewStateSerialization
{

	static final String WINDOW_TAG = "Window";

	/**
	 * Key to the view type name. Value is a string.
	 */
	static final String VIEW_TYPE_KEY = "Type";

	/**
	 * Key to the parameter that stores the frame position for
	 * {@link MastodonFrameView}s. Value is and <code>int[]</code> array of 4
	 * elements: x, y, width and height.
	 */
	static final String FRAME_POSITION_KEY = "FramePosition";

	/**
	 * Key that specifies whether the settings panel is visible or not.
	 */
	static final String SETTINGS_PANEL_VISIBLE_KEY = "SettingsPanelVisible";

	/**
	 * Key to the lock group id. Value is an int.
	 */
	static final String GROUP_HANDLE_ID_KEY = "LockGroupId";

	/**
	 * Key for the {@link ViewerState} in a BDV view. Value is a XML
	 * {@link Element} serialized from the state.
	 *
	 * @see ViewerPanelMamut#stateToXml()
	 * @see ViewerPanelMamut#stateFromXml(Element)
	 */
	static final String BDV_STATE_KEY = "BdvState";

	/**
	 * Key for the transform in a BDV view. Value is an
	 * {@link AffineTransform3D} instance.
	 */
	static final String BDV_TRANSFORM_KEY = "BdvTransform";

	/**
	 * Key for the transform in a TrackScheme view. Value is a
	 * {@link ScreenTransform} instance.
	 */
	static final String TRACKSCHEME_TRANSFORM_KEY = "TrackSchemeTransform";

	/**
	 * Key for the transform in a Grapher view. Value is a Grapher
	 * ScreenTransform instance.
	 */
	static final String GRAPHER_TRANSFORM_KEY = "GrapherTransform";

	/**
	 * Key that specifies whether a table only display the selection or the
	 * whole model. Boolean instance.
	 */
	static final String TABLE_SELECTION_ONLY = "TableSelectionOnly";

	/**
	 * Key that specifies whether a table is currently showing the vertex table.
	 * If <code>false</code>, then the edge table is displayed.
	 */
	static final String TABLE_DISPLAYING_VERTEX_TABLE = "TableVertexTableDisplayed";

	/**
	 * Key that specifies what table is currently showing in the table view.
	 * Values are <code>String</code> that points to a tab name in the tabbed
	 * pane.
	 */
	static final String TABLE_DISPLAYED = "TableDisplayed";

	/**
	 * Key to the parameter that stores the vertex table displayed rectangle.
	 * Value is and <code>int[]</code> array of 4 elements: x, y, width and
	 * height.
	 */
	static final String TABLE_VERTEX_TABLE_VISIBLE_POS = "TableVertexTableVisibleRect";

	/**
	 * Key to the parameter that stores the table displayed position. Value is
	 * and <code>int[]</code> array of 2 elements: x, y.
	 */
	static final String TABLE_VISIBLE_POS = "TableVisibleRect";

	/**
	 * Key to the parameter that stores the GUI states of multiple tables. Value
	 * is a <code>List<Map<String, Object>></code>.
	 */
	static final String TABLE_ELEMENT = "Tables";

	/**
	 * Key to the parameter that stores the table name in a table GUI state.
	 * Value is a <code>String</code>.
	 */
	static final String TABLE_NAME = "TableName";

	/**
	 * Key to the parameter that stores the edge table displayed rectangle.
	 * Value is and <code>int[]</code> array of 4 elements: x, y, width and
	 * height.
	 */
	static final String TABLE_EDGE_TABLE_VISIBLE_POS = "TableEdgeTableVisibleRect";

	/**
	 * Key that specifies whether we do not use a special coloring scheme on the
	 * view. If <code>true</code>, then we do not use a special coloring scheme.
	 *
	 * @see #TAG_SET_KEY
	 * @see #FEATURE_COLOR_MODE_KEY
	 */
	static final String NO_COLORING_KEY = "NoColoring";

	/**
	 * Key that specifies the name of the tag-set to use for coloring scheme
	 * based on tag-sets. A non-<code>null</code> value means the coloring
	 * scheme is based on tag-sets.
	 *
	 * @see #NO_COLORING_KEY
	 * @see #FEATURE_COLOR_MODE_KEY
	 */
	static final String TAG_SET_KEY = "TagSet";

	/**
	 * Key that specifies the name of the feature color mode to use for coloring
	 * scheme based on feature color modes. A non-<code>null</code> value means
	 * the coloring scheme is based on feature values.
	 *
	 * @see #NO_COLORING_KEY
	 * @see #TAG_SET_KEY
	 */
	static final String FEATURE_COLOR_MODE_KEY = "FeatureColorMode";

	/**
	 * Key that specifies the name of the chosen context provider. Values are
	 * strings.
	 */
	static final String CHOSEN_CONTEXT_PROVIDER_KEY = "ContextProvider";

	/**
	 * Key that specifies whether the colorbar is visible.
	 */
	static final String COLORBAR_VISIBLE_KEY = "ColorbarVisible";

	/**
	 * Key that specifies the colorbar position. Values are {@link Position}
	 * enum values.
	 */
	static final String COLORBAR_POSITION_KEY = "ColorbarPosition";

	/**
	 * Key that specifies settings specific to the branch-graph view in a common
	 * view. Values are <code>Map<String, Object></code>.
	 */
	static final String BRANCH_GRAPH = "BranchGraph";

	static void toXml( final Map< String, Object > map, final Element element )
	{
		for ( final Entry< String, Object > entry : map.entrySet() )
		{
			final Element el = toXml( entry.getKey(), entry.getValue() );
			element.addContent( el );
		}
	}

	@SuppressWarnings( "unchecked" )
	static Element toXml( final String key, final Object value )
	{
		final Element el;
		if ( value instanceof Integer )
			el = XmlHelpers.intElement( key, ( Integer ) value );
		else if ( value instanceof int[] )
			el = XmlHelpers.intArrayElement( key, ( int[] ) value );
		else if ( value instanceof Double )
			el = XmlHelpers.doubleElement( key, ( Double ) value );
		else if ( value instanceof double[] )
			el = XmlHelpers.doubleArrayElement( key, ( double[] ) value );
		else if ( value instanceof AffineGet )
			el = XmlHelpers.affineTransform3DElement( key, ( AffineGet ) value );
		else if ( value instanceof Boolean )
			el = XmlHelpers.booleanElement( key, ( Boolean ) value );
		else if ( value instanceof String )
		{
			el = new Element( key );
			el.setText( value.toString() );
		}
		else if ( value instanceof ScreenTransform )
		{
			final ScreenTransform t = ( ScreenTransform ) value;
			el = XmlHelpers.doubleArrayElement( key, new double[] {
					t.getMinX(),
					t.getMaxX(),
					t.getMinY(),
					t.getMaxY(),
					t.getScreenWidth(),
					t.getScreenHeight()
			} );
		}
		else if ( value instanceof org.mastodon.views.grapher.datagraph.ScreenTransform )
		{
			final org.mastodon.views.grapher.datagraph.ScreenTransform t = ( org.mastodon.views.grapher.datagraph.ScreenTransform ) value;
			el = XmlHelpers.doubleArrayElement( key, new double[] {
					t.getMinX(),
					t.getMaxX(),
					t.getMinY(),
					t.getMaxY(),
					t.getScreenWidth(),
					t.getScreenHeight()
			} );
		}
		else if ( value instanceof Position )
		{
			el = new Element( key );
			el.setText( ( ( Position ) value ).name() );
		}
		else if ( value instanceof Element )
		{
			el = new Element( key );
			el.setContent( ( Element ) value );
		}
		else if ( value instanceof Map )
		{
			el = new Element( key );
			toXml( ( Map< String, Object > ) value, el );
		}
		else if ( value instanceof List )
		{
			el = new Element( key );
			final List< Object > os = ( List< Object > ) value;
			for ( final Object o : os )
			{
				final Element child = toXml( key, o );
				el.addContent( child );
			}
		}
		else
		{
			System.err.println( "Do not know how to serialize object " + value + " for key " + key + "." );
			el = null;
		}
		return el;
	}

	private static void getGuiStateGrapher( final MamutViewGrapher view, final Map< String, Object > guiState )
	{
		final DataDisplayPanel< Spot, Link > dataDisplayPanel = view.getDataDisplayPanel();

		// Transform.
		final org.mastodon.views.grapher.datagraph.ScreenTransform t = dataDisplayPanel.getScreenTransform().get();
		guiState.put( GRAPHER_TRANSFORM_KEY, t );

		// Coloring.
		final ColoringModelMain< Spot, Link, BranchSpot, BranchLink > coloringModel = view.getColoringModel();
		getColoringState( coloringModel, guiState );

		// Colorbar.
		final ColorBarOverlay colorBarOverlay = view.getColorBarOverlay();
		getColorBarOverlayState( colorBarOverlay, guiState );

		// Context provider.
		guiState.put( CHOSEN_CONTEXT_PROVIDER_KEY, view.getContextChooser().getChosenProvider().getName() );
	}

	/**
	 * Reads the coloring state of a view and stores it into the specified map.
	 * 
	 * @param coloringModel
	 *            the coloring model to read from.
	 * @param guiState
	 *            the map to store it to.
	 */
	private static void getColoringState( final ColoringModel coloringModel, final Map< String, Object > guiState )
	{
		final boolean noColoring = coloringModel.noColoring();
		guiState.put( NO_COLORING_KEY, noColoring );
		if ( !noColoring )
			if ( coloringModel.getTagSet() != null )
				guiState.put( TAG_SET_KEY, coloringModel.getTagSet().getName() );
			else if ( coloringModel.getFeatureColorMode() != null )
				guiState.put( FEATURE_COLOR_MODE_KEY, coloringModel.getFeatureColorMode().getName() );
	}

	private static void getColorBarOverlayState( final ColorBarOverlay colorBarOverlay, final Map< String, Object > guiState )
	{
		guiState.put( COLORBAR_VISIBLE_KEY, colorBarOverlay.isVisible() );
		guiState.put( COLORBAR_POSITION_KEY, colorBarOverlay.getPosition() );
	}

	/**
	 * Deserializes a GUI state from XML and recreate view windows as specified.
	 * 
	 * @param windowsEl
	 *            the XML element that stores the GUI state of a view.
	 * @param windowManager
	 *            the application {@link WindowManager}.
	 */
	static void fromXml( final Element windowsEl, final WindowManager windowManager )
	{
		// To deal later with context providers.
		final Map< String, ContextProvider< Spot > > contextProviders = new HashMap<>();
		final Map< ContextChooser< Spot >, String > contextChosers = new HashMap<>();

		final List< Element > viewEls = windowsEl.getChildren( WINDOW_TAG );
		for ( final Element viewEl : viewEls )
		{
			final Map< String, Object > guiState = xmlToMap( viewEl );
			final String typeStr = ( String ) guiState.get( VIEW_TYPE_KEY );
			switch ( typeStr )
			{

			case "MamutViewGrapher":
			{
				final MamutViewGrapher grapher = null; // TODO

				// Deal with context chooser.
				final String desiredProvider = ( String ) guiState.get( CHOSEN_CONTEXT_PROVIDER_KEY );
				if ( null != desiredProvider )
					contextChosers.put( grapher.getContextChooser(), desiredProvider );
				break;
			}

			default:
				System.err.println( "Deserializing GUI state: Unknown window type: " + typeStr + "." );
				continue;
			}
		}

		/*
		 * Loop again on context choosers and try to give them their desired
		 * context provider.
		 */

		for ( final ContextChooser< Spot > contextChooser : contextChosers.keySet() )
		{
			final String desiredContextProvider = contextChosers.get( contextChooser );
			final ContextProvider< Spot > contextProvider = contextProviders.get( desiredContextProvider );
			if ( null != contextProvider )
				contextChooser.choose( contextProvider );
		}
	}

	private static Map< String, Object > xmlToMap( final Element viewEl )
	{
		final Map< String, Object > guiState = new HashMap<>();
		final List< Element > children = viewEl.getChildren();
		for ( final Element el : children )
		{
			final String key = el.getName();
			final Object value;
			switch ( key )
			{
			case BDV_STATE_KEY:
				value = el;
				break;
			case BDV_TRANSFORM_KEY:
				value = XmlHelpers.getAffineTransform3D( viewEl, key );
				break;
			case FRAME_POSITION_KEY:
				final int[] pos = XmlHelpers.getIntArray( viewEl, key );
				value = sanitize( pos );
				break;
			case TAG_SET_KEY:
			case FEATURE_COLOR_MODE_KEY:
			case VIEW_TYPE_KEY:
			case CHOSEN_CONTEXT_PROVIDER_KEY:
				value = el.getTextTrim();
				break;
			case TRACKSCHEME_TRANSFORM_KEY:
			{
				final double[] arr = XmlHelpers.getDoubleArray( viewEl, key );
				value = new ScreenTransform( arr[ 0 ], arr[ 1 ], arr[ 2 ], arr[ 3 ], ( int ) arr[ 4 ], ( int ) arr[ 5 ] );
				break;
			}
			case GRAPHER_TRANSFORM_KEY:
			{
				final double[] arr = XmlHelpers.getDoubleArray( viewEl, key );
				value = new org.mastodon.views.grapher.datagraph.ScreenTransform( arr[ 0 ], arr[ 1 ], arr[ 2 ], arr[ 3 ], ( int ) arr[ 4 ], ( int ) arr[ 5 ] );
				break;
			}
			case TABLE_SELECTION_ONLY:
			case NO_COLORING_KEY:
			case SETTINGS_PANEL_VISIBLE_KEY:
			case COLORBAR_VISIBLE_KEY:
				value = XmlHelpers.getBoolean( viewEl, key );
				break;
			case COLORBAR_POSITION_KEY:
				final String str = XmlHelpers.getText( viewEl, key );
				value = Position.valueOf( str );
				break;
			case GROUP_HANDLE_ID_KEY:
			{
				value = XmlHelpers.getInt( viewEl, key );
				break;
			}
			case TABLE_ELEMENT:
			{
				final List< Element > els = el.getChildren();
				final List< Map< String, Object > > maps = new ArrayList<>( els.size() );
				for ( final Element child : els )
				{
					final String name = child.getChildTextTrim( TABLE_NAME );
					final int[] tablePos = XmlHelpers.getIntArray( child, TABLE_VISIBLE_POS );
					final Map< String, Object > m = new HashMap<>();
					m.put( TABLE_NAME, name );
					m.put( TABLE_VISIBLE_POS, tablePos );
					maps.add( m );
				}
				value = maps;
				break;
			}
			case TABLE_DISPLAYED:
				value = XmlHelpers.getText( viewEl, TABLE_DISPLAYED );
				break;
			case BRANCH_GRAPH:
				value = xmlToMap( el );
				break;
			default:
				System.err.println( "Unknown GUI config parameter: " + key + " found in GUI file." );
				continue;
			}
			guiState.put( key, value );
		}
		return guiState;
	}

	private static final int MIN_WIDTH = 200;

	private static final int MIN_HEIGHT = MIN_WIDTH;

	/**
	 * Makes sure the specified position array won't end in creating windows
	 * off-screen. We impose that a window is fully on *one* screen and not
	 * split over severals. We also impose a minimal size for the windows.
	 * <p>
	 * The pos array is { x, y, width, height }.
	 * 
	 * @param pos
	 *            the position array.
	 * @return the same position array.
	 */
	private static int[] sanitize( final int[] pos )
	{
		assert pos.length == 4;
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if ( null == ge )
			return pos;
		final GraphicsDevice sd[] = ge.getScreenDevices();
		if ( sd.length < 1 )
			return pos;

		// Window min size.
		pos[ 2 ] = Math.max( MIN_WIDTH, pos[ 2 ] );
		pos[ 3 ] = Math.max( MIN_HEIGHT, pos[ 3 ] );

		for ( final GraphicsDevice gd : sd )
		{
			final Rectangle bounds = gd.getDefaultConfiguration().getBounds();
			if ( bounds.contains( pos[ 0 ], pos[ 1 ], pos[ 2 ], pos[ 3 ] ) )
				// Fully in a screen, nothing to do.
				return pos;

			if ( bounds.contains( pos[ 0 ], pos[ 1 ] ) )
			{
				/*
				 * This window is on this screen, but exits it. First resize it
				 * so that it is not bigger than the screen.
				 */
				pos[ 2 ] = Math.min( bounds.width, pos[ 2 ] );
				pos[ 3 ] = Math.min( bounds.height, pos[ 3 ] );

				/*
				 * Then move it back so that its bottom right corner is in the
				 * screen.
				 */
				if ( pos[ 0 ] + pos[ 2 ] > bounds.x + bounds.width )
					pos[ 0 ] -= ( pos[ 0 ] - bounds.x + pos[ 2 ] - bounds.width );

				if ( pos[ 1 ] + pos[ 3 ] > bounds.y + bounds.height )
					pos[ 1 ] -= ( pos[ 1 ] - bounds.y + pos[ 3 ] - bounds.height );

				return pos;
			}
		}

		/*
		 * Ok we did not find a screen in which this window is. So we will put
		 * it in the first screen.
		 */
		final Rectangle bounds = sd[ 0 ].getDefaultConfiguration().getBounds();
		pos[ 0 ] = Math.max( bounds.x,
				Math.min( bounds.x + bounds.width - pos[ 2 ], pos[ 0 ] ) );
		pos[ 1 ] = Math.max( bounds.y,
				Math.min( bounds.y + bounds.height - pos[ 3 ], pos[ 1 ] ) );

		if ( bounds.contains( pos[ 0 ], pos[ 1 ], pos[ 2 ], pos[ 3 ] ) )
			// Fully in a screen, nothing to do.
			return pos;

		/*
		 * This window is on this screen, but exits it. First resize it so that
		 * it is not bigger than the screen.
		 */
		pos[ 2 ] = Math.min( bounds.width, pos[ 2 ] );
		pos[ 3 ] = Math.min( bounds.height, pos[ 3 ] );

		/*
		 * Then move it back so that its bottom right corner is in the screen.
		 */
		pos[ 0 ] -= ( pos[ 0 ] - bounds.x + pos[ 2 ] - bounds.width );
		pos[ 1 ] -= ( pos[ 1 ] - bounds.y + pos[ 3 ] - bounds.height );

		return pos;
	}
}
