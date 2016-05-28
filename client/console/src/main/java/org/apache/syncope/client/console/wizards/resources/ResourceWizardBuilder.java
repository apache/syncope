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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.ComponentTag;

/**
 * Resource wizard builder.
 */
public class ResourceWizardBuilder extends AbstractResourceWizardBuilder<ResourceTO> {

    private static final long serialVersionUID = 1734415311027284221L;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private boolean createFlag;

    public ResourceWizardBuilder(final ResourceTO resourceTO, final PageReference pageRef) {
        super(resourceTO, pageRef);
    }

    @Override
    public AjaxWizard<Serializable> build(final String id, final AjaxWizard.Mode mode) {
        this.createFlag = mode == AjaxWizard.Mode.CREATE;
        return super.build(id, mode);
    }

    @Override
    protected WizardModel buildModelSteps(final Serializable modelObject, final WizardModel wizardModel) {
        ResourceTO resourceTO = ResourceTO.class.cast(modelObject);
        wizardModel.add(new ResourceDetailsPanel(resourceTO, createFlag));
        wizardModel.add(new ResourceConnConfPanel(resourceTO, createFlag) {

            private static final long serialVersionUID = -1128269449868933504L;

            @Override
            protected Pair<Boolean, String> check(final AjaxRequestTarget target) {
                return resourceRestClient.check(modelObject);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                tag.append("class", "scrollable-tab-content", " ");
            }

        });
        wizardModel.add(new ResourceConnCapabilitiesPanel(
                resourceTO, connectorRestClient.read(resourceTO.getConnector()).getCapabilities()));

        wizardModel.add(new ResourceSecurityPanel(resourceTO));
        return wizardModel;
    }

    @Override
    protected ResourceTO onApplyInternal(final Serializable modelObject) {
        final ResourceTO resourceTO = ResourceTO.class.cast(modelObject);
        if (createFlag) {
            return resourceRestClient.create(resourceTO);
        } else {
            resourceRestClient.update(resourceTO);
            return resourceTO;
        }
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
