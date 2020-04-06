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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.ProvisioningPolicyTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class PolicyModalPanelBuilder<T extends PolicyTO> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final BaseModal<T> modal;

    private final PolicyType type;

    public PolicyModalPanelBuilder(
            final PolicyType type, final T policyTO, final BaseModal<T> modal, final PageReference pageRef) {

        super(policyTO, pageRef);
        this.type = type;
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
                return SyncopeWebApplication.get().getResourceProvider().get();
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
                        new PropertyModel<>(policyTO, "maxAuthenticationAttempts")));

                fields.add(new AjaxCheckBoxPanel(
                        "field",
                        "propagateSuspension",
                        new PropertyModel<>(policyTO, "propagateSuspension"),
                        false));

                fields.add(new AjaxPalettePanel.Builder<String>().setName("passthroughResources").build(
                        "field",
                        new PropertyModel<>(policyTO, "passthroughResources"),
                        new ListModel<>(resources.getObject())));
            } else if (policyTO instanceof PasswordPolicyTO) {
                fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                        "field",
                        "historyLength",
                        Integer.class,
                        new PropertyModel<>(policyTO, "historyLength")));

                fields.add(new AjaxCheckBoxPanel(
                        "field",
                        "allowNullPassword",
                        new PropertyModel<>(policyTO, "allowNullPassword"),
                        false));
            } else if (policyTO instanceof ProvisioningPolicyTO) {
                fields.add(new AjaxDropDownChoicePanel<>(
                        "field",
                        "conflictResolutionAction",
                        new PropertyModel<>(policyTO, "conflictResolutionAction")).
                        setChoices(List.of((Serializable[]) ConflictResolutionAction.values())));
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
        public void onSubmit(final AjaxRequestTarget target) {
            try {
                if (policyTO.getKey() == null) {
                    PolicyRestClient.createPolicy(type, policyTO);
                } else {
                    PolicyRestClient.updatePolicy(type, policyTO);
                }
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                Profile.this.modal.close(target);
            } catch (Exception e) {
                LOG.error("While creating/updating policy", e);
                SyncopeConsoleSession.get().onException(e);
            }
            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
