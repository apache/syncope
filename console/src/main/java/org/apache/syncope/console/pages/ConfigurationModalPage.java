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
package org.apache.syncope.console.pages;

import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.rest.ConfigurationRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.to.ConfigurationTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Connector form.
 */
public class ConfigurationModalPage extends BaseModalPage {

    private static final long serialVersionUID = -5266230025217580098L;

    @SpringBean
    private ConfigurationRestClient configurationsRestClient;

    private AjaxButton submit;

    /**
     * ConfigurationModalPage constructor.
     *
     * @param callPageRef base
     * @param window
     * @param configurationTO
     * @param createFlag true for CREATE and false for UPDATE operation
     */
    public ConfigurationModalPage(final PageReference callPageRef, final ModalWindow window,
            final ConfigurationTO configurationTO, final boolean createFlag) {

        Form form = new Form("form", new CompoundPropertyModel(configurationTO));

        final AjaxTextFieldPanel key = new AjaxTextFieldPanel("key", "key", new PropertyModel(configurationTO, "key"));
        form.add(key);
        key.setEnabled(createFlag);
        key.addRequiredLabel();

        final AjaxTextFieldPanel value = new AjaxTextFieldPanel("value", "value", new PropertyModel(configurationTO,
                "value"));
        form.add(value);

        submit = new IndicatingAjaxButton("apply", new Model<String>(getString("submit"))) {
            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    if (createFlag) {
                        configurationsRestClient.createConfiguration(configurationTO);
                    } else {
                        configurationsRestClient.updateConfiguration(configurationTO);
                    }

                    Configuration callerPage = (Configuration) callPageRef.getPage();
                    callerPage.setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    if (createFlag) {
                        error(getString("error_insert"));
                    } else {
                        error(getString("error_updating"));
                    }
                    target.add(feedbackPanel);
                    LOG.error("While creating or updating configuration {}", configurationTO, e);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {
            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Configuration", "create")
                : xmlRolesReader.getAllAllowedRoles("Configuration", "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        form.add(submit);

        add(form);
        add(new CloseOnESCBehavior(window));
    }
}
