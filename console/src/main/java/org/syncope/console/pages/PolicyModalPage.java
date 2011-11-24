/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.Arrays;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.console.pages.panels.PolicyBeanPanel;
import org.syncope.console.rest.PolicyRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.types.AbstractPolicySpec;
import org.syncope.types.AccountPolicySpec;
import org.syncope.types.PasswordPolicySpec;
import org.syncope.types.PolicyType;
import org.syncope.types.SyncPolicySpec;

/**
 * Modal window with Resource form.
 */
public class PolicyModalPage<T extends PolicyTO> extends BaseModalPage {

    private static final long serialVersionUID = -7325772767481076679L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    public PolicyModalPage(
            final ModalWindow window, final T policyTO) {

        super();

        final Form form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        final AjaxTextFieldPanel policyid = new AjaxTextFieldPanel(
                "id", "id",
                new PropertyModel<String>(policyTO, "id"), false);
        policyid.setEnabled(false);
        policyid.setStyleShet(
                "ui-widget-content ui-corner-all short_fixedsize");
        form.add(policyid);

        final AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                "description", "description",
                new PropertyModel<String>(policyTO, "description"), false);
        description.addRequiredLabel();
        description.setStyleShet(
                "ui-widget-content ui-corner-all medium_dynamicsize");
        form.add(description);

        final AjaxDropDownChoicePanel<PolicyType> type =
                new AjaxDropDownChoicePanel<PolicyType>(
                "type", "type",
                new PropertyModel<PolicyType>(policyTO, "type"), false);

        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                type.setChoices(Arrays.asList(new PolicyType[]{
                            PolicyType.GLOBAL_ACCOUNT, PolicyType.ACCOUNT}));
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                type.setChoices(Arrays.asList(new PolicyType[]{
                            PolicyType.GLOBAL_PASSWORD, PolicyType.PASSWORD}));
                break;

            case GLOBAL_SYNC:
            case SYNC:
                type.setChoices(Arrays.asList(new PolicyType[]{
                            PolicyType.GLOBAL_SYNC, PolicyType.SYNC}));

            default:
        }

        type.setChoiceRenderer(new PolicyTypeRenderer());

        type.addRequiredLabel();
        form.add(type);

        final AbstractPolicySpec policy = getPolicySpecification(policyTO);

        form.add(new PolicyBeanPanel("panel", policy));

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("apply")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                setPolicySpecification(policyTO, policy);

                try {
                    if (policyTO.getId() > 0) {
                        policyRestClient.updatePolicy(policyTO);
                    } else {
                        policyRestClient.createPolicy(policyTO);
                    }

                    window.close(target);
                } catch (Exception e) {
                    LOG.error("While creating policy", e);

                    error(getString("operation_error"));
                    target.add(getPage().get("feedback"));
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(getPage().get("feedback"));
            }
        };

        form.add(submit);
    }

    private AbstractPolicySpec getPolicySpecification(final PolicyTO policyTO) {
        AbstractPolicySpec spec;

        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                spec = ((AccountPolicyTO) policyTO).getSpecification() != null
                        ? ((AccountPolicyTO) policyTO).getSpecification()
                        : new AccountPolicySpec();
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                spec = ((PasswordPolicyTO) policyTO).getSpecification() != null
                        ? ((PasswordPolicyTO) policyTO).getSpecification()
                        : new PasswordPolicySpec();
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                spec = ((SyncPolicyTO) policyTO).getSpecification() != null
                        ? ((SyncPolicyTO) policyTO).getSpecification()
                        : new SyncPolicySpec();
        }

        return spec;
    }

    private void setPolicySpecification(
            final PolicyTO policyTO, final AbstractPolicySpec specification) {

        switch (policyTO.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                ((AccountPolicyTO) policyTO).setSpecification(
                        (AccountPolicySpec) specification);
                break;

            case GLOBAL_PASSWORD:
            case PASSWORD:
                ((PasswordPolicyTO) policyTO).setSpecification(
                        (PasswordPolicySpec) specification);
                break;

            case GLOBAL_SYNC:
            case SYNC:
                ((SyncPolicyTO) policyTO).setSpecification(
                        (SyncPolicySpec) specification);

            default:
        }
    }

    private class PolicyTypeRenderer extends ChoiceRenderer<PolicyType> {

        private static final long serialVersionUID = -8993265421104002134L;

        @Override
        public Object getDisplayValue(final PolicyType object) {
            return getString(object.name());
        }
    };
}
