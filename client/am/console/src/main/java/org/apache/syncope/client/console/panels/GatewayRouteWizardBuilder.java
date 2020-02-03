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

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import org.apache.syncope.client.console.rest.GatewayRouteRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class GatewayRouteWizardBuilder extends BaseAjaxWizardBuilder<GatewayRouteTO> {

    private static final long serialVersionUID = 2060352959114706419L;

    public GatewayRouteWizardBuilder(final GatewayRouteTO route, final PageReference pageRef) {
        super(route, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final GatewayRouteTO modelObject) {
        if (modelObject.getKey() == null) {
            GatewayRouteRestClient.create(modelObject);
        } else {
            GatewayRouteRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final GatewayRouteTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Predicates(modelObject));
        wizardModel.add(new Filters(modelObject));
        return wizardModel;
    }

    public static class Profile extends WizardStep {

        private static final long serialVersionUID = 8610155719550948702L;

        public Profile(final GatewayRouteTO route) {
            add(new AjaxTextFieldPanel(
                    "name", "name", new PropertyModel<>(route, "name"), false).
                    addRequiredLabel().setEnabled(true));

            add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).build(
                    "order", "order", Integer.class, new PropertyModel<>(route, "order")).
                    setRequired(true));

            AjaxTextFieldPanel target = new AjaxTextFieldPanel(
                    "target", "target", new IModel<String>() {

                private static final long serialVersionUID = 1015030402166681242L;

                @Override
                public String getObject() {
                    return route.getTarget() == null ? null : route.getTarget().toASCIIString();
                }

                @Override
                public void setObject(final String object) {
                    if (object == null) {
                        route.setTarget(null);
                    } else {
                        route.setTarget(URI.create(object));
                    }
                }
            }, false);
            target.addRequiredLabel().setEnabled(true);
            target.getField().add(new UrlValidator(new String[] { "http", "https" }));
            add(target);

            add(new AjaxDropDownChoicePanel<>(
                    "status", "status", new PropertyModel<>(route, "status")).
                    setChoices(List.of((Serializable[]) GatewayRouteStatus.values())));
        }
    }

    public static class Predicates extends WizardStep {

        private static final long serialVersionUID = 5934389493874714599L;

        public Predicates(final GatewayRouteTO route) {
            super(new ResourceModel("predicates"), Model.of());
            add(new GatewayRoutePredicatePanel("predicates", new ListModel<>(route.getPredicates())));
        }
    }

    public static class Filters extends WizardStep {

        private static final long serialVersionUID = -6552124285142294023L;

        public Filters(final GatewayRouteTO route) {
            super(new ResourceModel("filters"), Model.of());
            add(new GatewayRouteFilterPanel("filters", new ListModel<>(route.getFilters())));
        }
    }
}
