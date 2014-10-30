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

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.Mode;
import org.apache.syncope.console.pages.panels.AttributesPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

public class ConfModalPage extends BaseModalPage {

    private static final long serialVersionUID = 3524777398688399977L;

    public ConfModalPage(final PageReference pageRef, final ModalWindow window, final WebMarkupContainer parameters) {
        super();

        MetaDataRoleAuthorizationStrategy.authorize(
                parameters, ENABLE, xmlRolesReader.getAllAllowedRoles("Configuration", "list"));
        final ConfTO conf = confRestClient.list();

        final Form<ConfTO> form = new Form<ConfTO>("confForm");
        form.setModel(new CompoundPropertyModel<ConfTO>(conf));

        form.add(new AttributesPanel("paramAttrs", conf, form, Mode.ADMIN));

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ConfTO updatedConf = (ConfTO) form.getModelObject();

                try {
                    for (AttributeTO attr : updatedConf.getAttrs()) {
                        if (attr.getValues().isEmpty()
                                || attr.getValues().equals(Collections.singletonList(StringUtils.EMPTY))) {

                            confRestClient.delete(attr.getSchema());
                        } else {
                            confRestClient.set(attr);
                        }
                    }

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    window.close(target);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, xmlRolesReader.getAllAllowedRoles("Configuration", "set"));
        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, xmlRolesReader.getAllAllowedRoles("Configuration", "delete"));
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }
}
