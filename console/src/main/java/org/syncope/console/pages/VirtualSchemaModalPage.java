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
package org.syncope.console.pages;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.to.VirtualSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

/**
 * Modal window with Schema form.
 */
public class VirtualSchemaModalPage extends AbstractSchemaModalPage {

    private static final long serialVersionUID = 5979623248182851337L;

    public VirtualSchemaModalPage(String kind) {
        super(kind);
    }

    @Override
    public void setSchemaModalPage(
            final PageReference callerPageRef,
            final ModalWindow window,
            AbstractBaseBean schema,
            final boolean createFlag) {

        if (schema == null) {
            schema = new VirtualSchemaTO();
        }

        final Form schemaForm = new Form("form");

        schemaForm.setModel(new CompoundPropertyModel(schema));

        final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                "name", getString("name"),
                new PropertyModel<String>(schema, "name"), false);
        name.addRequiredLabel();

        name.setEnabled(createFlag);

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                VirtualSchemaTO schemaTO =
                        (VirtualSchemaTO) form.getDefaultModelObject();

                try {
                    if (createFlag) {
                        restClient.createVirtualSchema(kind, schemaTO);
                    } else {
                        restClient.updateVirtualSchema(kind, schemaTO);
                    }
                    if (callerPageRef.getPage() instanceof BasePage) {
                        ((BasePage) callerPageRef.getPage()).setModalResult(
                                true);
                    }

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, allowedRoles);

        schemaForm.add(name);

        schemaForm.add(submit);

        add(schemaForm);
    }
}
