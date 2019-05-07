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

import java.util.Set;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class ResourceConnCapabilitiesPanel extends WizardStep {

    private static final long serialVersionUID = -114632577031611754L;

    public ResourceConnCapabilitiesPanel(
            final ResourceTO resourceTO, final Set<ConnectorCapability> connectorCapabilities) {
        super();
        setOutputMarkupId(true);

        if (!resourceTO.isOverrideCapabilities() && resourceTO.getCapabilitiesOverride().isEmpty()) {
            resourceTO.getCapabilitiesOverride().addAll(connectorCapabilities);
        }

        final CapabilitiesPanel connCapabilitiesPanel = new CapabilitiesPanel(
                new PropertyModel<>(resourceTO, "capabilitiesOverride"));
        connCapabilitiesPanel.setEnabled(resourceTO.isOverrideCapabilities());
        add(connCapabilitiesPanel);

        final AjaxCheckBoxPanel overrideCapabilities = new AjaxCheckBoxPanel(
                "overrideCapabilities",
                new ResourceModel("overrideCapabilities", "overrideCapabilities").getObject(),
                new PropertyModel<>(resourceTO, "overrideCapabilities"));
        overrideCapabilities.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                connCapabilitiesPanel.setEnabled(overrideCapabilities.getField().getModelObject());
                target.add(ResourceConnCapabilitiesPanel.this);
            }
        });
        add(overrideCapabilities);
    }

}
