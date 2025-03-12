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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class ResourceConnCapabilitiesPanel extends WizardStep {

    private static final long serialVersionUID = -114632577031611754L;

    public ResourceConnCapabilitiesPanel(
            final ResourceTO resourceTO,
            final Set<ConnectorCapability> connectorCapabilities) {

        super();
        setOutputMarkupId(true);

        CapabilitiesPanel connCapabilitiesPanel = new CapabilitiesPanel(new IModel<List<ConnectorCapability>>() {

            private static final long serialVersionUID = -3729760042701500963L;

            @Override
            public List<ConnectorCapability> getObject() {
                return resourceTO.getCapabilitiesOverride().
                        map(co -> {
                            List<ConnectorCapability> object = new ArrayList<>(co);
                            if (co.isEmpty()) {
                                co.addAll(connectorCapabilities);
                            }
                            return object;
                        }).
                    orElseGet(List::of);
            }

            @Override
            public void setObject(final List<ConnectorCapability> object) {
                resourceTO.setCapabilitiesOverride(Optional.of(new HashSet<>(object)));
            }
        });
        connCapabilitiesPanel.setEnabled(!resourceTO.getCapabilitiesOverride().isEmpty());
        add(connCapabilitiesPanel);

        AjaxCheckBoxPanel overrideCapabilities = new AjaxCheckBoxPanel(
                "overrideCapabilities",
                new ResourceModel("overrideCapabilities", "overrideCapabilities").getObject(),
                new IModel<Boolean>() {

            private static final long serialVersionUID = -7523036477975507287L;

            @Override
            public Boolean getObject() {
                return !resourceTO.getCapabilitiesOverride().isEmpty();
            }

            @Override
            public void setObject(final Boolean object) {
                if (BooleanUtils.isTrue(object)) {
                    resourceTO.setCapabilitiesOverride(Optional.of(new HashSet<>()));
                } else {
                    resourceTO.setCapabilitiesOverride(Optional.empty());
                }
            }
        });
        overrideCapabilities.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                connCapabilitiesPanel.setEnabled(overrideCapabilities.getField().getModelObject());
                if (overrideCapabilities.getField().getModelObject()) {
                    resourceTO.setCapabilitiesOverride(Optional.of(connectorCapabilities));
                } else {
                    resourceTO.setCapabilitiesOverride(Optional.empty());
                }
                target.add(ResourceConnCapabilitiesPanel.this);
            }
        });
        add(overrideCapabilities);
    }
}
