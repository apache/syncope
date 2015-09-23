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

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal.ModalEvent;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
    private static final Logger LOG = LoggerFactory.getLogger(ResourceDetailsPanel.class);

    @SpringBean
    private ConnectorRestClient connRestClient;

    private ConnInstanceTO connInstanceTO;

    public ResourceDetailsPanel(final String id, final ResourceTO resourceTO, final List<String> actionClassNames,
            final boolean createFlag) {

        super(id);
        setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(true);
        add(container);

        final AjaxTextFieldPanel resourceName = new AjaxTextFieldPanel("name", new ResourceModel("name", "name").
                getObject(), new PropertyModel<String>(resourceTO, "key"));

        resourceName.setEnabled(createFlag);
        resourceName.addRequiredLabel();
        container.add(resourceName);

        final AjaxCheckBoxPanel enforceMandatoryCondition = new AjaxCheckBoxPanel("enforceMandatoryCondition",
                new ResourceModel("enforceMandatoryCondition", "enforceMandatoryCondition").getObject(),
                new PropertyModel<Boolean>(resourceTO, "enforceMandatoryCondition"));
        container.add(enforceMandatoryCondition);

        final AjaxCheckBoxPanel propagationPrimary = new AjaxCheckBoxPanel("propagationPrimary", new ResourceModel(
                "propagationPrimary", "propagationPrimary").getObject(), new PropertyModel<Boolean>(resourceTO,
                        "propagationPrimary"));
        container.add(propagationPrimary);

        final SpinnerFieldPanel<Integer> propagationPriority = new SpinnerFieldPanel<>(
                "propagationPriority",
                "propagationPriority",
                Integer.class,
                new PropertyModel<Integer>(resourceTO, "propagationPriority"));
        container.add(propagationPriority);

        final AjaxDropDownChoicePanel<PropagationMode> propagationMode = new AjaxDropDownChoicePanel<>(
                "propagationMode", new ResourceModel("propagationMode", "propagationMode").getObject(),
                new PropertyModel<PropagationMode>(resourceTO, "propagationMode"));
        propagationMode.setChoices(Arrays.asList(PropagationMode.values()));
        container.add(propagationMode);

        final AjaxCheckBoxPanel randomPwdIfNotProvided = new AjaxCheckBoxPanel("randomPwdIfNotProvided",
                new ResourceModel("randomPwdIfNotProvided", "randomPwdIfNotProvided").getObject(),
                new PropertyModel<Boolean>(resourceTO, "randomPwdIfNotProvided"));
        container.add(randomPwdIfNotProvided);

        final AjaxDropDownChoicePanel<String> template
                = new AjaxDropDownChoicePanel<>("panel", "panel", new Model<String>());
        template.setChoices(actionClassNames);
        template.setNullValid(true);
        template.setRequired(true);

        final MultiFieldPanel<String> actions = new MultiFieldPanel<>(
                "actionsClasses",
                "actionsClasses",
                new PropertyModel<List<String>>(resourceTO, "propagationActionsClassNames"),
                template);

        container.add(actions);

        final AjaxDropDownChoicePanel<TraceLevel> createTraceLevel = new AjaxDropDownChoicePanel<>(
                "createTraceLevel", new ResourceModel("createTraceLevel", "createTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "createTraceLevel"));
        createTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        container.add(createTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> updateTraceLevel = new AjaxDropDownChoicePanel<>(
                "updateTraceLevel", new ResourceModel("updateTraceLevel", "updateTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "updateTraceLevel"));
        updateTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        container.add(updateTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> deleteTraceLevel = new AjaxDropDownChoicePanel<>(
                "deleteTraceLevel", new ResourceModel("deleteTraceLevel", "deleteTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "deleteTraceLevel"));
        deleteTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        container.add(deleteTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> syncTraceLevel = new AjaxDropDownChoicePanel<>(
                "syncTraceLevel", new ResourceModel("syncTraceLevel", "syncTraceLevel").getObject(),
                new PropertyModel<TraceLevel>(resourceTO, "syncTraceLevel"));
        syncTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        container.add(syncTraceLevel);

        final IModel<List<ConnInstanceTO>> connectors = new LoadableDetachableModel<List<ConnInstanceTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnInstanceTO> load() {
                return connRestClient.getAllConnectors();
            }
        };

        connInstanceTO = getConectorInstanceTO(connectors.getObject(), resourceTO);

        final AjaxDropDownChoicePanel<ConnInstanceTO> conn = new AjaxDropDownChoicePanel<>("connector",
                new ResourceModel("connector", "connector").getObject(),
                new PropertyModel<ConnInstanceTO>(this, "connInstanceTO"));
        conn.setChoices(connectors.getObject());
        conn.setChoiceRenderer(new ChoiceRenderer<ConnInstanceTO>("displayName", "key"));

        conn.getField().setModel(new IModel<ConnInstanceTO>() {

            private static final long serialVersionUID = -4202872830392400310L;

            @Override
            public ConnInstanceTO getObject() {
                return connInstanceTO;
            }

            @Override
            public void setObject(final ConnInstanceTO connector) {
                resourceTO.setConnector(connector.getKey());
                connInstanceTO = connector;
            }

            @Override
            public void detach() {
            }
        });

        conn.addRequiredLabel();
        conn.setEnabled(false);

        conn.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(getPage(), Broadcast.BREADTH, new DetailsModEvent(target));
            }
        });

        container.add(conn);

        add(new AnnotatedBeanPanel("systeminformation", resourceTO));
    }

    /**
     * Get the connetorTO linked to the resource.
     *
     * @param connectorTOs list of all connectors.
     * @param resourceTO resource.
     * @return selected connector instance: in case of no connectors available, null; in case of new resource
     * specification, the first on connector available
     */
    private ConnInstanceTO getConectorInstanceTO(final List<ConnInstanceTO> connectorTOs, final ResourceTO resourceTO) {
        if (connectorTOs.isEmpty()) {
            resourceTO.setConnector(null);
            return null;
        } else {
            // use the first element as default
            ConnInstanceTO res = connectorTOs.get(0);

            for (ConnInstanceTO to : connectorTOs) {
                if (Long.valueOf(to.getKey()).equals(resourceTO.getConnector())) {
                    res = to;
                }
            }

            // in case of no match
            resourceTO.setConnector(res.getKey());

            return res;
        }
    }

    /**
     * Connector instance modification event.
     */
    public static class DetailsModEvent extends ModalEvent {

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
