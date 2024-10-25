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
package org.apache.syncope.client.console.wizards.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourceDetailsPanel extends WizardStep {

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    protected AjaxDropDownChoicePanel<String> connector;

    protected final IModel<List<String>> propagationActions = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdMImplementationType.PROPAGATION_ACTIONS).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final IModel<List<String>> provisionSorters = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 4659376149825914247L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdMImplementationType.PROVISION_SORTER).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    public ResourceDetailsPanel(final ResourceTO resourceTO, final boolean createFlag) {
        super();
        setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(true);
        add(container);

        container.add(new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                new ResourceModel(Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME).getObject(),
                new PropertyModel<>(resourceTO, Constants.KEY_FIELD_NAME),
                false).addRequiredLabel().setEnabled(createFlag));

        container.add(new AjaxCheckBoxPanel(
                "enforceMandatoryCondition",
                new ResourceModel("enforceMandatoryCondition", "enforceMandatoryCondition").getObject(),
                new PropertyModel<>(resourceTO, "enforceMandatoryCondition"),
                false));

        container.add(new AjaxNumberFieldPanel.Builder<Integer>().build(
                "propagationPriority",
                "propagationPriority",
                Integer.class,
                new PropertyModel<>(resourceTO, "propagationPriority")));

        container.add(new AjaxPalettePanel.Builder<String>().
                setAllowMoveAll(true).setAllowOrder(true).
                build("propagationActions",
                        new PropertyModel<>(resourceTO, "propagationActions"),
                        new ListModel<>(propagationActions.getObject())).
                setOutputMarkupId(true));

        container.add(new AjaxDropDownChoicePanel<>(
                "createTraceLevel",
                new ResourceModel("createTraceLevel", "createTraceLevel").getObject(),
                new PropertyModel<>(resourceTO, "createTraceLevel"),
                false).
                setChoices(Arrays.stream(TraceLevel.values()).collect(Collectors.toList())).setNullValid(false));

        container.add(new AjaxDropDownChoicePanel<>(
                "provisionSorter", "provisionSorter",
                new PropertyModel<>(resourceTO, "provisionSorter"), false).
                setChoices(provisionSorters));

        container.add(new AjaxDropDownChoicePanel<>(
                "updateTraceLevel",
                new ResourceModel("updateTraceLevel", "updateTraceLevel").getObject(),
                new PropertyModel<>(resourceTO, "updateTraceLevel"),
                false).
                setChoices(Arrays.stream(TraceLevel.values()).collect(Collectors.toList())).setNullValid(false));

        container.add(new AjaxDropDownChoicePanel<>(
                "deleteTraceLevel",
                new ResourceModel("deleteTraceLevel", "deleteTraceLevel").getObject(),
                new PropertyModel<>(resourceTO, "deleteTraceLevel"),
                false).
                setChoices(Arrays.stream(TraceLevel.values()).collect(Collectors.toList())).setNullValid(false));

        container.add(new AjaxDropDownChoicePanel<>(
                "provisioningTraceLevel",
                new ResourceModel("provisioningTraceLevel", "provisioningTraceLevel").getObject(),
                new PropertyModel<>(resourceTO, "provisioningTraceLevel"),
                false).
                setChoices(Arrays.stream(TraceLevel.values()).collect(Collectors.toList())).setNullValid(false));

        if (resourceTO.getConnector() != null) {
            container.add(new AjaxTextFieldPanel(
                    "connector",
                    new ResourceModel("connector", "connector").getObject(),
                    new Model<>(resourceTO.getConnectorDisplayName()),
                    false).addRequiredLabel().setEnabled(false));
        } else {
            connector = new AjaxDropDownChoicePanel<>(
                    "connector",
                    new ResourceModel("connector", "connector").getObject(),
                    new PropertyModel<>(resourceTO, "connector"), false);
            Map<String, String> connectorsMap = connectorRestClient.getAllConnectors().stream().
                    collect(Collectors.toMap(ConnInstanceTO::getKey, ConnInstanceTO::getDisplayName));
            connector.setChoices(connectorsMap.keySet().stream().sorted().collect(Collectors.toList()));
            connector.setChoiceRenderer(new IChoiceRenderer<>() {

                private static final long serialVersionUID = 91313845533448846L;

                private final Map<String, String> valueMap = connectorsMap;

                @Override
                public String getDisplayValue(final String value) {
                    return Optional.ofNullable(valueMap.get(value)).orElse(null);
                }

                @Override
                public String getIdValue(final String value, final int i) {
                    return value;
                }

                @Override
                public String getObject(final String id, final IModel<? extends List<? extends String>> choices) {
                    return id;
                }
            });
            connector.addRequiredLabel();
            connector.setOutputMarkupId(true);
            connector.getField().setOutputMarkupId(true);
            container.add(connector);
        }
    }

    public AjaxDropDownChoicePanel<String> getConnector() {
        return connector;
    }
}
