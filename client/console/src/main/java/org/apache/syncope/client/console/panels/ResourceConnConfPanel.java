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
package org.apache.syncope.client.console.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.pages.BaseModalPage;
import org.apache.syncope.client.console.pages.ResourceModalPage.ResourceEvent;
import org.apache.syncope.client.console.panels.ResourceDetailsPanel.DetailsModEvent;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel.MultiValueSelectorEvent;
import org.apache.syncope.client.console.wicket.markup.html.list.ConnConfPropertyListView;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourceConnConfPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    private ConnectorRestClient restClient;

    private final ResourceTO resourceTO;

    private final boolean createFlag;

    private List<ConnConfProperty> connConfProperties;

    private final WebMarkupContainer connConfPropContainer;

    private final AjaxButton check;

    public ResourceConnConfPanel(final String id, final ResourceTO resourceTO, final boolean createFlag) {
        super(id);
        setOutputMarkupId(true);

        this.createFlag = createFlag;
        this.resourceTO = resourceTO;

        connConfProperties = getConnConfProperties();

        connConfPropContainer = new WebMarkupContainer("connectorPropertiesContainer");
        connConfPropContainer.setOutputMarkupId(true);
        add(connConfPropContainer);

        /*
         * the list of overridable connector properties
         */
        final ListView<ConnConfProperty> connPropView = new ConnConfPropertyListView("connectorProperties",
                new PropertyModel<List<ConnConfProperty>>(this, "connConfProperties"),
                false, resourceTO.getConnConfProperties());
        connPropView.setOutputMarkupId(true);
        connConfPropContainer.add(connPropView);

        check = new IndicatingAjaxButton("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -4199438518229098169L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ResourceTO to = (ResourceTO) form.getModelObject();

                if (restClient.check(to)) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                ((BaseModalPage) getPage()).getFeedbackPanel().refresh(target);
            }
        };

        check.setEnabled(!connConfProperties.isEmpty());
        check.setVisible(!connConfProperties.isEmpty());

        connConfPropContainer.add(check);
    }

    /**
     * Get overridable properties.
     *
     * @return overridable properties.
     */
    private List<ConnConfProperty> getConnConfProperties() {
        List<ConnConfProperty> props = new ArrayList<>();
        Long connectorKey = resourceTO.getConnector();
        if (connectorKey != null && connectorKey > 0) {
            for (ConnConfProperty property : restClient.getConnectorProperties(connectorKey)) {
                if (property.isOverridable()) {
                    props.add(property);
                }
            }
        }
        if (createFlag || resourceTO.getConnConfProperties().isEmpty()) {
            resourceTO.getConnConfProperties().clear();
        } else {
            Map<String, ConnConfProperty> valuedProps = new HashMap<>();
            for (ConnConfProperty prop : resourceTO.getConnConfProperties()) {
                valuedProps.put(prop.getSchema().getName(), prop);
            }

            for (int i = 0; i < props.size(); i++) {
                if (valuedProps.containsKey(props.get(i).getSchema().getName())) {
                    props.set(i, valuedProps.get(props.get(i).getSchema().getName()));
                }
            }
        }

        // re-order properties
        Collections.sort(props);

        return props;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        AjaxRequestTarget target = null;
        if (event.getPayload() instanceof DetailsModEvent) {
            // connector change: update properties and forward event
            target = ((ResourceEvent) event.getPayload()).getTarget();

            connConfProperties = getConnConfProperties();
            check.setEnabled(!connConfProperties.isEmpty());

            target.add(connConfPropContainer);
        } else if (event.getPayload() instanceof MultiValueSelectorEvent) {
            // multi value connector property change: forward event
            target = ((MultiValueSelectorEvent) event.getPayload()).getTarget();
        }

        if (target != null) {
            send(getPage(), Broadcast.BREADTH, new ConnConfModEvent(target, connConfProperties));
        }
    }

    /**
     * Connector configuration properties modification event.
     */
    public static class ConnConfModEvent extends ResourceEvent {

        private final List<ConnConfProperty> configuration;

        /**
         * Constructor.
         *
         * @param target request target.
         * @param configuration connector configuration properties.
         */
        public ConnConfModEvent(final AjaxRequestTarget target, final List<ConnConfProperty> configuration) {
            super(target);
            this.configuration = configuration;
        }

        public List<ConnConfProperty> getConfiguration() {
            return configuration;
        }
    }
}
