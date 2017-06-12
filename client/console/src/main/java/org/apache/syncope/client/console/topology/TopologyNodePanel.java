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
package org.apache.syncope.client.console.topology;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.topology.TopologyNode.Kind;
import org.apache.syncope.client.console.topology.TopologyTogglePanel.UpdateEvent;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class TopologyNodePanel extends Panel implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -8775095410207013913L;

    private final Label label;

    private final TopologyNode node;

    protected enum Status {
        ACTIVE,
        INACTIVE

    }

    public TopologyNodePanel(final String id, final TopologyNode node) {
        super(id);
        this.node = node;

        final String resourceName = node.getDisplayName().length() > 14
                ? node.getDisplayName().subSequence(0, 10) + "..."
                : node.getDisplayName();

        label = new Label("label", resourceName);
        label.setOutputMarkupId(true);
        add(label);

        final String title;

        switch (node.getKind()) {
            case SYNCOPE:
                title = "";
                add(new AttributeAppender("class", "topology_root", " "));
                break;
            case CONNECTOR_SERVER:
                title = node.getDisplayName();
                add(new AttributeAppender("class", "topology_cs", " "));
                break;
            case FS_PATH:
                title = node.getDisplayName();
                add(new AttributeAppender("class", "topology_cs", " "));
                break;
            case CONNECTOR:
                title = (StringUtils.isBlank(node.getConnectionDisplayName())
                        ? "" : node.getConnectionDisplayName() + ":") + node.getDisplayName();
                add(new AttributeAppender("class", "topology_conn", " "));
                break;
            default:
                title = node.getDisplayName().length() > 14 ? node.getDisplayName() : "";
                add(new AttributeAppender("class", "topology_res", " "));
        }

        if (StringUtils.isNotEmpty(title)) {
            add(AttributeModifier.append("data-original-title", title));
        }

        this.setMarkupId(node.getDisplayName());
    }

    @Override
    public final MarkupContainer add(final Component... childs) {
        return super.add(childs);
    }

    @Override
    public final Component add(final Behavior... behaviors) {
        return super.add(behaviors);
    }

    @Override
    public final Component setMarkupId(final String markupId) {
        return super.setMarkupId(markupId);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return Constants.VEIL_INDICATOR_MARKUP_ID;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof UpdateEvent) {
            final UpdateEvent updateEvent = UpdateEvent.class.cast(event.getPayload());
            final String key = updateEvent.getKey();
            final AjaxRequestTarget target = updateEvent.getTarget();

            if (node.getKind() == Kind.CONNECTOR && key.equalsIgnoreCase(node.getKey())) {
                String displayName = new ConnectorRestClient().read(key).getDisplayName();

                final String resourceName = displayName.length() > 14
                        ? displayName.subSequence(0, 10) + "..."
                        : displayName;

                label.setDefaultModelObject(resourceName);
                target.add(label);
            }
        }
    }

}
