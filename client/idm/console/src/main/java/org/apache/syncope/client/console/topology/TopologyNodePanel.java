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
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.topology.TopologyNode.Kind;
import org.apache.syncope.client.console.topology.TopologyTogglePanel.UpdateEvent;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TopologyNodePanel extends Panel implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -8775095410207013913L;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    protected final Label label;

    protected final TopologyNode node;

    protected enum Status {
        ACTIVE,
        INACTIVE

    }

    public TopologyNodePanel(final String id, final TopologyNode node, final boolean errored) {
        super(id);
        this.node = node;

        label = new Label("label", StringUtils.abbreviate(node.getDisplayName(), 10));
        label.setOutputMarkupId(true);
        add(label);

        String title;
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
                        ? "" : node.getConnectionDisplayName() + ':') + node.getDisplayName();
                if (errored) {
                    add(new AttributeAppender("class", "topology_conn_errored", " "));
                } else {
                    add(new AttributeAppender("class", "topology_conn", " "));
                }
                break;

            case RESOURCE:
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
    public String getAjaxIndicatorMarkupId() {
        return Constants.VEIL_INDICATOR_MARKUP_ID;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof UpdateEvent) {
            UpdateEvent updateEvent = UpdateEvent.class.cast(event.getPayload());
            String key = updateEvent.getKey();

            if (node.getKind() == Kind.CONNECTOR && key.equalsIgnoreCase(node.getKey())) {
                ConnInstanceTO conn = connectorRestClient.read(key);

                // [SYNCOPE-1233]
                String displayName = StringUtils.isBlank(conn.getDisplayName())
                        ? conn.getBundleName() : conn.getDisplayName();

                label.setDefaultModelObject(StringUtils.abbreviate(displayName, 10));
                updateEvent.getTarget().add(label);
                node.setDisplayName(displayName);
            }
        }
    }
}
