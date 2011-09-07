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
import org.syncope.console.pages.panels.PasswordPoliciesPanel;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.console.pages.panels.PolicyBeanPanel;
import org.syncope.console.rest.PolicyRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.PolicyType;

/**
 * Modal window with Resource form.
 */
public class PasswordPolicyModalPage extends BaseModalPage {

    private static final long serialVersionUID = -7325772767481076679L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    public PasswordPolicyModalPage(
            final PasswordPoliciesPanel basePage,
            final ModalWindow window,
            final PasswordPolicyTO policyTO) {

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

        type.setChoices(Arrays.asList(new PolicyType[]{
                    PolicyType.GLOBAL_PASSWORD, PolicyType.PASSWORD}));
        type.addRequiredLabel();
        form.add(type);

        final PasswordPolicy policy = policyTO.getSpecification() != null
                ? policyTO.getSpecification() : new PasswordPolicy();

        form.add(new PolicyBeanPanel("panel", policy));

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new Model(getString("apply"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                policyTO.setSpecification(policy);

                try {
                    if (policyTO.getId() > 0) {
                        final PasswordPolicyMod policyMod =
                                new PasswordPolicyMod();
                        policyMod.setId(policyTO.getId());
                        policyMod.setType(policyTO.getType());
                        policyMod.setSpecification(policyTO.getSpecification());
                        policyMod.setDescription(policyTO.getDescription());

                        policyRestClient.updatePasswordPolicy(policyMod);
                    } else {
                        policyRestClient.createPasswordPolicy(policyTO);
                    }

                    window.close(target);
                } catch (Exception e) {
                    LOG.error("While creating policy", e);

                    error(getString("operation_error"));
                    target.addComponent(getPage().get("feedback"));
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(getPage().get("feedback"));
            }
        };

        form.add(submit);
    }
}
