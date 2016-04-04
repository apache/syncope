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

import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class ResourceConnCapabilitiesPanel extends Panel {

    private static final long serialVersionUID = -114632577031611754L;

    public ResourceConnCapabilitiesPanel(
            final String id, final IModel<ResourceTO> model, final Set<ConnectorCapability> connectorCapabilities) {

        super(id, model);
        setOutputMarkupId(true);

        if (!model.getObject().isOverrideCapabilities() && model.getObject().getCapabilitiesOverride().isEmpty()) {
            model.getObject().getCapabilitiesOverride().addAll(connectorCapabilities);
        }

        final ConnCapabilitiesPanel connCapabilitiesPanel = new ConnCapabilitiesPanel(
                "capabilitiesOverride",
                new PropertyModel<List<ConnectorCapability>>(model.getObject(), "capabilitiesOverride"));
        connCapabilitiesPanel.setEnabled(model.getObject().isOverrideCapabilities());
        add(connCapabilitiesPanel);

        final AjaxCheckBoxPanel overrideCapabilities = new AjaxCheckBoxPanel(
                "overrideCapabilities",
                new ResourceModel("overrideCapabilities", "overrideCapabilities").getObject(),
                new PropertyModel<Boolean>(model, "overrideCapabilities"));
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
