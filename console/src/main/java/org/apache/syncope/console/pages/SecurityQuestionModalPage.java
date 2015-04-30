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

import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
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

class SecurityQuestionModalPage extends BaseModalPage {

    private static final long serialVersionUID = -6709838862698327502L;

    @SpringBean
    private SecurityQuestionRestClient restClient;

    public SecurityQuestionModalPage(final PageReference pageRef, final ModalWindow window,
            final SecurityQuestionTO securityQuestionTO, final boolean createFlag) {

        final Form<SecurityQuestionTO> form =
                new Form<SecurityQuestionTO>(FORM, new CompoundPropertyModel<SecurityQuestionTO>(securityQuestionTO));

        final AjaxTextFieldPanel contentFieldPanel =
                new AjaxTextFieldPanel("content", "content", new PropertyModel<String>(securityQuestionTO, "content"));
        contentFieldPanel.setRequired(true);
        form.add(contentFieldPanel);

        AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<String>(getString(SUBMIT))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    if (createFlag) {
                        restClient.create(securityQuestionTO);
                    } else {
                        restClient.update(securityQuestionTO);
                    }
                    info(getString(Constants.OPERATION_SUCCEEDED));

                    Configuration callerPage = (Configuration) pageRef.getPage();
                    callerPage.setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientException scee) {
                    error(getString(Constants.ERROR) + ": " + scee.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);

        String allowedRoles = createFlag
                ? xmlRolesReader.getEntitlement("SecurityQuestion", "create")
                : xmlRolesReader.getEntitlement("SecurityQuestion", "update");
        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        form.add(submit);
        form.setDefaultButton(submit);

        form.add(cancel);

        add(form);
    }
}
