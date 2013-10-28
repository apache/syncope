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

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.to.DerSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.JexlHelpUtil;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Schema form.
 */
public class DerivedSchemaModalPage extends AbstractSchemaModalPage {

    private static final long serialVersionUID = 6668789770131753386L;

    public DerivedSchemaModalPage(final AttributableType kind) {
        super(kind);
    }

    @Override
    public void setSchemaModalPage(final PageReference pageRef, final ModalWindow window,
            AbstractBaseBean schema, final boolean createFlag) {

        if (schema == null) {
            schema = new DerSchemaTO();
        }

        final Form schemaForm = new Form(FORM);

        schemaForm.setModel(new CompoundPropertyModel(schema));

        final AjaxTextFieldPanel name = new AjaxTextFieldPanel("name", getString("name"), new PropertyModel<String>(
                schema, "name"));
        name.addRequiredLabel();

        final AjaxTextFieldPanel expression = new AjaxTextFieldPanel("expression", getString("expression"),
                new PropertyModel<String>(schema, "expression"));
        expression.addRequiredLabel();

        final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");
        schemaForm.add(jexlHelp);


        final AjaxLink questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        schemaForm.add(questionMarkJexlHelp);


        name.setEnabled(createFlag);

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                DerSchemaTO schemaTO = (DerSchemaTO) form.getDefaultModelObject();

                try {
                    if (createFlag) {
                        schemaRestClient.createDerSchema(kind, schemaTO);
                    } else {
                        schemaRestClient.updateDerSchema(kind, schemaTO);
                    }

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    window.close(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(
                false);

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Schema", "create")
                : xmlRolesReader.getAllAllowedRoles("Schema", "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        schemaForm.add(name);

        schemaForm.add(expression);

        schemaForm.add(submit);

        schemaForm.add(cancel);

        add(schemaForm);
    }
}
