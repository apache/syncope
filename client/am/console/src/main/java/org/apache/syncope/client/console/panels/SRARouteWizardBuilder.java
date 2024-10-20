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
import java.util.Optional;
import org.apache.syncope.client.console.rest.SRARouteRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class SRARouteWizardBuilder extends BaseAjaxWizardBuilder<SRARouteTO> {

    private static final long serialVersionUID = 2060352959114706419L;

    protected final SRARouteRestClient sraRouteRestClient;

    public SRARouteWizardBuilder(
            final SRARouteTO route,
            final SRARouteRestClient sraRouteRestClient,
            final PageReference pageRef) {

        super(route, pageRef);
        this.sraRouteRestClient = sraRouteRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final SRARouteTO modelObject) {
        if (modelObject.getKey() == null) {
            sraRouteRestClient.create(modelObject);
        } else {
            sraRouteRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SRARouteTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Predicates(modelObject));
        wizardModel.add(new Filters(modelObject));
        return wizardModel;
    }

    public static class Profile extends WizardStep {

        private static final long serialVersionUID = 8610155719550948702L;

        public Profile(final SRARouteTO route) {
            add(new AjaxTextFieldPanel(
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(route, Constants.NAME_FIELD_NAME), false).
                    addRequiredLabel().setEnabled(true));

            AjaxTextFieldPanel target = new AjaxTextFieldPanel("target", "target", new IModel<>() {

                private static final long serialVersionUID = 1015030402166681242L;

                @Override
                public String getObject() {
                    return Optional.ofNullable(route.getTarget()).map(URI::toASCIIString).orElse(null);
                }

                @Override
                public void setObject(final String object) {
                    route.setTarget(Optional.ofNullable(object).map(URI::create).orElse(null));
                }
            }, false);
            target.addRequiredLabel().setEnabled(true);
            target.getField().add(new UrlValidator(new String[] { "http", "https" }));
            add(target);

            AjaxTextFieldPanel error = new AjaxTextFieldPanel("error", "error", new IModel<>() {

                private static final long serialVersionUID = 1015030402166681242L;

                @Override
                public String getObject() {
                    return route.getError() == null ? null : route.getError().toASCIIString();
                }

                @Override
                public void setObject(final String object) {
                    if (object == null) {
                        route.setError(null);
                    } else {
                        route.setError(URI.create(object));
                    }
                }
            }, false);
            error.getField().add(new UrlValidator(new String[] { "http", "https" }));
            add(error);

            AjaxDropDownChoicePanel<SRARouteType> type =
                    new AjaxDropDownChoicePanel<>("type", "type", new PropertyModel<>(route, "type"));
            type.setChoices(List.of(SRARouteType.values()));
            type.addRequiredLabel().setEnabled(true);
            add(type);

            add(new AjaxCheckBoxPanel("logout", "logout", new PropertyModel<>(route, "logout")));

            AjaxTextFieldPanel postLogout = new AjaxTextFieldPanel("postLogout", "postLogout", new IModel<>() {

                private static final long serialVersionUID = 1015030402166681242L;

                @Override
                public String getObject() {
                    return route.getPostLogout() == null ? null : route.getPostLogout().toASCIIString();
                }

                @Override
                public void setObject(final String object) {
                    if (object == null) {
                        route.setPostLogout(null);
                    } else {
                        route.setPostLogout(URI.create(object));
                    }
                }
            }, false);
            postLogout.getField().add(new UrlValidator(new String[] { "http", "https" }));
            add(postLogout);

            add(new AjaxCheckBoxPanel("csrf", "csrf", new PropertyModel<>(route, "csrf")));

            add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
                    "order", "order", Integer.class, new PropertyModel<>(route, "order")).
                    setRequired(true));
        }
    }

    private static class Predicates extends WizardStep {

        private static final long serialVersionUID = 5934389493874714599L;

        Predicates(final SRARouteTO route) {
            super(new ResourceModel("predicates"), Model.of());
            add(new SRARoutePredicatePanel("predicates", new ListModel<>(route.getPredicates())));
        }
    }

    private static class Filters extends WizardStep {

        private static final long serialVersionUID = -6552124285142294023L;

        Filters(final SRARouteTO route) {
            super(new ResourceModel("filters"), Model.of());
            add(new SRARouteFilterPanel("filters", new ListModel<>(route.getFilters())));
        }
    }
}
