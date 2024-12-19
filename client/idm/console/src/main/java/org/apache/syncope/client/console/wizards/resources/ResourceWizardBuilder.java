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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Resource wizard builder.
 */
public class ResourceWizardBuilder extends AbstractResourceWizardBuilder<ResourceTO> {

    private static final long serialVersionUID = 1734415311027284221L;

    protected final ResourceRestClient resourceRestClient;

    protected final ConnectorRestClient connectorRestClient;

    protected boolean createFlag;

    public ResourceWizardBuilder(
            final ResourceTO resourceTO,
            final ResourceRestClient resourceRestClient,
            final ConnectorRestClient connectorRestClient,
            final PageReference pageRef) {

        super(resourceTO, pageRef);

        this.resourceRestClient = resourceRestClient;
        this.connectorRestClient = connectorRestClient;
    }

    @Override
    public AjaxWizard<Serializable> build(final String id, final AjaxWizard.Mode mode) {
        this.createFlag = mode == AjaxWizard.Mode.CREATE;
        return super.build(id, mode);
    }

    @Override
    protected WizardModel buildModelSteps(final Serializable modelObject, final WizardModel wizardModel) {
        ResourceTO resourceTO = ResourceTO.class.cast(modelObject);
        ResourceDetailsPanel resourceDetailsPanel = new ResourceDetailsPanel(resourceTO, createFlag);

        ResourceConnConfPanel resourceConnConfPanel = new ResourceConnConfPanel(resourceTO) {

            private static final long serialVersionUID = -1128269449868933504L;

            @Override
            protected Pair<Boolean, String> check(final AjaxRequestTarget target) {
                return resourceRestClient.check(modelObject);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                tag.append("class", "scrollable-tab-content", " ");
            }
        };

        if (createFlag && resourceDetailsPanel.getConnector() != null) {
            resourceDetailsPanel.getConnector().getField().add(
                    new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = 4600298808455564695L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    resourceTO.setConnector(resourceDetailsPanel.getConnector().getModelObject());

                    LoadableDetachableModel<List<ConnConfProperty>> model = new LoadableDetachableModel<>() {

                        private static final long serialVersionUID = -2965284931860212687L;

                        @Override
                        protected List<ConnConfProperty> load() {
                            List<ConnConfProperty> confOverride = resourceConnConfPanel.getConnProperties(resourceTO);
                            resourceTO.setConfOverride(Optional.of(confOverride));
                            return confOverride;
                        }
                    };
                    resourceConnConfPanel.setConfPropertyListView(model, true);
                    target.add(resourceConnConfPanel.getCheck().setVisible(true).setEnabled(true));
                }
            });
        }
        wizardModel.add(resourceDetailsPanel);
        wizardModel.add(resourceConnConfPanel);
        Optional.ofNullable(resourceTO.getConnector()).ifPresentOrElse(
                conn -> wizardModel.add(new ResourceConnCapabilitiesPanel(
                        resourceTO, connectorRestClient.read(conn).getCapabilities())),
                () -> wizardModel.add(new ResourceConnCapabilitiesPanel(resourceTO, Set.of())));

        wizardModel.add(new ResourceSecurityPanel(resourceTO));
        return wizardModel;
    }

    @Override
    protected ResourceTO onApplyInternal(final Serializable modelObject) {
        ResourceTO resourceTO = (ResourceTO) modelObject;
        if (createFlag) {
            resourceTO = resourceRestClient.create(resourceTO);
        } else {
            resourceRestClient.update(resourceTO);
        }
        return resourceTO;
    }

    @Override
    protected Serializable getCreateCustomPayloadEvent(final Serializable afterObject, final AjaxRequestTarget target) {
        final ResourceTO actual = ResourceTO.class.cast(afterObject);
        return new CreateEvent(
                actual.getKey(),
                actual.getKey(),
                TopologyNode.Kind.RESOURCE,
                actual.getConnector(),
                target);
    }
}
