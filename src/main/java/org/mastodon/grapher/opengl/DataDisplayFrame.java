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

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureModel.FeatureModelListener;
import org.mastodon.mamut.feature.SpotPositionFeature;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.util.FeatureUtils;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.mastodon.views.grapher.display.FeatureGraphConfig.GraphDataItemsSource;
import org.mastodon.views.grapher.display.FeatureSpecPair;
import org.mastodon.views.grapher.display.GrapherSidePanel;

public class DataDisplayFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	private final GrapherSidePanel sidePanel;

	public DataDisplayFrame( final int nSources, final ModelGraph graph, final SelectionModel< Spot, Link > selection, final FeatureModel featureModel )
	{
		super( "Grapher" );

		/*
		 * Plot panel.
		 */

		final DataLayout layout = new DataLayout();
		final DataDisplayPanel dataDisplayPanel = new DataDisplayPanel( layout, featureModel, graph, selection );

		/*
		 * Side panel.
		 */

		final ContextChooser< Spot > contextChooser = new ContextChooser<>( dataDisplayPanel );
		sidePanel = new GrapherSidePanel( nSources, contextChooser );
		sidePanel.btnPlot.addActionListener( e -> dataDisplayPanel.plot( sidePanel.getGraphConfig() ) );

		final FeatureModelListener featureModelListener = () -> sidePanel.setFeatures(
				FeatureUtils.collectFeatureMap( featureModel, Spot.class ),
				FeatureUtils.collectFeatureMap( featureModel, Link.class ) );
		featureModel.listeners().add( featureModelListener );
		featureModelListener.featureModelChanged();

		final FeatureSpecPair spvx = new FeatureSpecPair( SpotPositionFeature.SPEC,
				SpotPositionFeature.PROJECTION_SPECS.get( 0 ), 0, false, false );
		final FeatureSpecPair spvy = new FeatureSpecPair( SpotPositionFeature.SPEC,
				SpotPositionFeature.PROJECTION_SPECS.get( 1 ), 0, false, false );
		final FeatureGraphConfig fgc = new FeatureGraphConfig( spvx, spvy, GraphDataItemsSource.CONTEXT, false );
		sidePanel.setGraphConfig( fgc );

//		dataDisplayPanel.plot( fgc, featureModel );

		/*
		 * Main panel is a split pane.
		 */

		final JSplitPane mainPanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
				sidePanel, dataDisplayPanel );
		mainPanel.setOneTouchExpandable( true );
		mainPanel.setBorder( null );
		mainPanel.setDividerLocation( 250 );
		add( mainPanel, BorderLayout.CENTER );

		pack();
		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		setVisible( true );
	}
}
