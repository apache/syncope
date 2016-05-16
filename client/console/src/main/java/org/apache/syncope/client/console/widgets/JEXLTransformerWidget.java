/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.widgets;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconTypeBuilder;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wizards.resources.JEXLTransformersTogglePanel;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class JEXLTransformerWidget extends AlertWidget<String> {

    private static final long serialVersionUID = 7667120094526529934L;

    private final MappingItemTO mapItem;

    private final JEXLTransformersTogglePanel transformers;

    public JEXLTransformerWidget(
            final String id,
            final MappingItemTO mapItem,
            final JEXLTransformersTogglePanel transformers) {

        super(id);
        setOutputMarkupId(true);

        this.mapItem = mapItem;
        this.transformers = transformers;
        
        this.latestAlertsList.setVisible(false);
    }

    @Override
    protected IModel<List<String>> getLatestAlerts() {
        return new ListModel<String>() {

            private static final long serialVersionUID = -2583290457773357445L;

            @Override
            public List<String> getObject() {
                List<String> result = new ArrayList<>();
                if (StringUtils.isNotBlank(mapItem.getPropagationJEXLTransformer())) {
                    result.add(mapItem.getPropagationJEXLTransformer());
                }
                if (StringUtils.isNotBlank(mapItem.getPullJEXLTransformer())) {
                    result.add(mapItem.getPullJEXLTransformer());
                }
                return result;
            }
        };
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        return new AjaxLink<String>(linkid) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                transformers.setMappingItem(target, JEXLTransformerWidget.this.mapItem);
                transformers.toggle(target, true);
            }
        };
    }

    @Override
    protected Icon getIcon(final String iconid) {
        return new Icon(
                iconid, FontAwesomeIconTypeBuilder.on(FontAwesomeIconTypeBuilder.FontAwesomeGraphic.repeat).build());
    }
}
