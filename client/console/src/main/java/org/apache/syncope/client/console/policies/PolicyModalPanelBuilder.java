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
package org.apache.syncope.client.console.policies;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.WizardModalPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class PolicyModalPanelBuilder<T extends AbstractPolicyTO> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final BaseModal<T> modal;

    private final PolicyRestClient restClient = new PolicyRestClient();

    public PolicyModalPanelBuilder(final T policyTO, final BaseModal<T> modal, final PageReference pageRef) {
        super(policyTO, pageRef);
        this.modal = modal;
    }

    @Override
    public WizardModalPanel<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return new Profile(newModelObject(), modal, pageRef);
    }

    public class Profile extends AbstractModalPanel<T> {

        private static final long serialVersionUID = -3043839139187792810L;

        private final T policyTO;

        private final LoadableDetachableModel<List<String>> resources = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ResourceRestClient().list().stream().map(EntityTO::getKey).collect(Collectors.toList());
            }
        };

        public Profile(final T policyTO, final BaseModal<T> modal, final PageReference pageRef) {
            super(modal, pageRef);
            modal.setFormModel(policyTO);

            this.policyTO = policyTO;

            final List<Component> fields = new ArrayList<>();

            FieldPanel<String> description = new AjaxTextFieldPanel("field", "description",
                    new PropertyModel<>(policyTO, "description"), false);
            description.setRequired(true);
            fields.add(description);

            if (policyTO instanceof AccountPolicyTO) {
                fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                        "field",
                        "maxAuthenticationAttempts",
                        Integer.class,
                        new PropertyModel<Integer>(policyTO, "maxAuthenticationAttempts")));

                fields.add(new AjaxCheckBoxPanel(
                        "field",
                        "propagateSuspension",
                        new PropertyModel<>(policyTO, "propagateSuspension"),
                        false));

                fields.add(new AjaxPalettePanel.Builder<String>().setName("passthroughResources").build(
                        "field",
                        new PropertyModel<List<String>>(policyTO, "passthroughResources"),
                        new ListModel<String>(resources.getObject())));
            }

            if (policyTO instanceof PasswordPolicyTO) {
                fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                        "field",
                        "historyLength",
                        Integer.class,
                        new PropertyModel<Integer>(policyTO, "historyLength")));

                fields.add(new AjaxCheckBoxPanel(
                        "field",
                        "allowNullPassword",
                        new PropertyModel<>(policyTO, "allowNullPassword"),
                        false));
            }

            add(new ListView<Component>("fields", fields) {

                private static final long serialVersionUID = -9180479401817023838L;

                @Override
                protected void populateItem(final ListItem<Component> item) {
                    item.add(item.getModelObject());
                }

            });
        }

        @Override
        public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            try {
                if (policyTO.getKey() == null) {
                    restClient.createPolicy(policyTO);
                } else {
                    restClient.updatePolicy(policyTO);
                }
                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                Profile.this.modal.close(target);
            } catch (Exception e) {
                LOG.error("While creating/updating policy", e);
                SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                        ? e.getClass().getName() : e.getMessage());
            }
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

}
