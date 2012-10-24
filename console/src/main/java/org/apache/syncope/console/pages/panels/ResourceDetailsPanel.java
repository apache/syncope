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
package org.apache.syncope.console.pages.panels;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.console.pages.ResourceModalPage.ResourceEvent;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.TraceLevel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceDetailsPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ResourceDetailsPanel.class);

    @SpringBean
    private ConnectorRestClient connRestClient;

    private ConnInstanceTO connInstanceTO;

    public ResourceDetailsPanel(final String id, final ResourceTO resourceTO, final List<String> actionClassNames,
            final boolean createFlag) {

        super(id);
        setOutputMarkupId(true);

        final IModel<List<ConnInstanceTO>> connectors = new LoadableDetachableModel<List<ConnInstanceTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnInstanceTO> load() {
                return connRestClient.getAllConnectors();
            }
        };

        connInstanceTO = getConectorInstanceTO(connectors.getObject(), resourceTO);

        final AjaxTextFieldPanel resourceName = new AjaxTextFieldPanel("name", new ResourceModel("name", "name").
                getObject(), new PropertyModel<String>(resourceTO, "name"));

        resourceName.setEnabled(createFlag);
        resourceName.addRequiredLabel();
        add(resourceName);

        final AjaxCheckBoxPanel enforceMandatoryCondition = new AjaxCheckBoxPanel("enforceMandatoryCondition",
                new ResourceModel("enforceMandatoryCondition", "enforceMandatoryCondition").getObject(),
                new PropertyModel<Boolean>(resourceTO, "enforceMandatoryCondition"));
        add(enforceMandatoryCondition);

        final AjaxCheckBoxPanel propagationPrimary = new AjaxCheckBoxPanel("propagationPrimary", new ResourceModel(
                "propagationPrimary", "propagationPrimary").getObject(), new PropertyModel<Boolean>(resourceTO,
                "propagationPrimary"));
        add(propagationPrimary);

        final AjaxNumberFieldPanel propagationPriority = new AjaxNumberFieldPanel("propagationPriority",
                new ResourceModel("propagationPriority", "propagationPriority").getObject(), new PropertyModel<Number>(
                resourceTO, "propagationPriority"), Integer.class);
        add(propagationPriority);

        final AjaxDropDownChoicePanel<PropagationMode> propagationMode = new AjaxDropDownChoicePanel<PropagationMode>(
                "propagationMode", new ResourceModel("propagationMode", "propagationMode").getObject(),
                new PropertyModel<PropagationMode>(resourceTO, "propagationMode"));
        propagationMode.setChoices(Arrays.asList(PropagationMode.values()));
        add(propagationMode);

        final AjaxDropDownChoicePanel<String> actionsClassName = new AjaxDropDownChoicePanel<String>(
                "actionsClassName", new ResourceModel("actionsClass", "actionsClass").getObject(),
                new PropertyModel<String>(resourceTO, "actionsClassName"));
        actionsClassName.setChoices(actionClassNames);
        actionsClassName.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
        add(actionsClassName);

        final AjaxDropDownChoicePanel<TraceLevel> createTraceLevel = new AjaxDropDownChoicePanel<TraceLevel>(
                "createTraceLevel", new ResourceModel("createTraceLevel", "createTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "createTraceLevel"));
        createTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        add(createTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> updateTraceLevel = new AjaxDropDownChoicePanel<TraceLevel>(
                "updateTraceLevel", new ResourceModel("updateTraceLevel", "updateTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "updateTraceLevel"));
        updateTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        add(updateTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> deleteTraceLevel = new AjaxDropDownChoicePanel<TraceLevel>(
                "deleteTraceLevel", new ResourceModel("deleteTraceLevel", "deleteTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "deleteTraceLevel"));
        deleteTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        add(deleteTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> syncTraceLevel = new AjaxDropDownChoicePanel<TraceLevel>(
                "syncTraceLevel", new ResourceModel("syncTraceLevel", "syncTraceLevel").getObject(), 
                new PropertyModel<TraceLevel>(resourceTO, "syncTraceLevel"));
        syncTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        add(syncTraceLevel);

        final AjaxCheckBoxPanel resetToken = new AjaxCheckBoxPanel("resetToken", new ResourceModel("resetToken",
                "resetToken").getObject(), new Model(null));

        resetToken.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget art) {
                if (resetToken.getModelObject()) {
                    resourceTO.setSyncToken(null);
                }
            }
        });
        add(resetToken);

        final AjaxDropDownChoicePanel<ConnInstanceTO> conn = new AjaxDropDownChoicePanel<ConnInstanceTO>("connector",
                new ResourceModel("connector", "connector").getObject(), 
                new PropertyModel<ConnInstanceTO>(this, "connInstanceTO"));
        conn.setChoices(connectors.getObject());
        conn.setChoiceRenderer(new ChoiceRenderer("displayName", "id"));

        conn.getField().setModel(new IModel<ConnInstanceTO>() {

            private static final long serialVersionUID = -4202872830392400310L;

            @Override
            public ConnInstanceTO getObject() {
                return connInstanceTO;
            }

            @Override
            public void setObject(final ConnInstanceTO connector) {
                resourceTO.setConnectorId(connector.getId());
                connInstanceTO = connector;
            }

            @Override
            public void detach() {
            }
        });

        conn.addRequiredLabel();
        conn.setEnabled(createFlag);

        conn.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(getPage(), Broadcast.BREADTH, new DetailsModEvent(target));
            }
        });

        add(conn);
    }

    /**
     * Get the connetorTO linked to the resource.
     *
     * @param connectorTOs list of all connectors.
     * @param resourceTO resource.
     * @return selected connector instance. In case of no connectors available return null. In case of new resource
     * specification return the first on connector available.
     */
    private ConnInstanceTO getConectorInstanceTO(final List<ConnInstanceTO> connectorTOs, final ResourceTO resourceTO) {

        if (connectorTOs.isEmpty()) {

            resourceTO.setConnectorId(null);
            return null;

        } else {

            // use the first element as default
            ConnInstanceTO res = connectorTOs.get(0);

            for (ConnInstanceTO to : connectorTOs) {
                if (Long.valueOf(to.getId()).equals(resourceTO.getConnectorId())) {
                    res = to;
                }
            }

            // in case of no match
            resourceTO.setConnectorId(res.getId());

            return res;
        }

    }

    /**
     * Connector instance modification event.
     */
    public static class DetailsModEvent extends ResourceEvent {

        /**
         * Constructor.
         *
         * @param target request target.
         */
        public DetailsModEvent(final AjaxRequestTarget target) {
            super(target);
        }
    }
}
