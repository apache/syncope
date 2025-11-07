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
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wizards.mapping.JEXLTransformersTogglePanel;
import org.apache.syncope.common.lib.to.Item;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.AbstractLink;

public class JEXLTransformerWidget extends AlertWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private final Item item;

    private final JEXLTransformersTogglePanel transformers;

    public JEXLTransformerWidget(
            final String id,
            final Item item,
            final JEXLTransformersTogglePanel transformers) {

        super(id);

        this.item = item;
        this.transformers = transformers;
    }

    @Override
    protected long getLatestAlertsSize() {
        long size = 0;
        if (StringUtils.isNotBlank(item.getPropagationJEXLTransformer())) {
            size++;
        }
        if (StringUtils.isNotBlank(item.getPullJEXLTransformer())) {
            size++;
        }
        return size;
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        return new AjaxLink<String>(linkid) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                transformers.setItem(target, JEXLTransformerWidget.this.item);
                transformers.toggle(target, true);
            }
        };
    }

    @Override
    protected Icon getIcon(final String iconid) {
        return new Icon(iconid, FontAwesome5IconType.redo_s);
    }
}
