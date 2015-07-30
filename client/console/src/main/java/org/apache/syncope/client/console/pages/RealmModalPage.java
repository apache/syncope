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
package org.apache.syncope.client.console.pages;

import static org.apache.syncope.client.console.pages.AbstractBasePage.CANCEL;
import static org.apache.wicket.Component.ENABLE;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.RealmDetails;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmModalPage<T extends AnyTO> extends BaseModalPage {

    private static final long serialVersionUID = -4285220460543213901L;

    private static final int ROWS_PER_PAGE = 10;

    protected RealmTO realmTO;

    private final PageReference pageRef;

    private final ModalWindow window;

    @SpringBean
    private RealmRestClient realmRestClient;
    
    private final String parentPath;

    public RealmModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement) {

        super();

        this.pageRef = pageRef;
        this.window = window;
        this.realmTO = realmTO;
        this.parentPath = parentPath;

        final Form<RealmTO> form = new Form<RealmTO>("RealmForm");
        form.setModel(new CompoundPropertyModel<RealmTO>(realmTO));

        RealmDetails realmDetail = new RealmDetails("details", realmTO);
        if (SyncopeConsoleSession.get().owns(entitlement)) {
            MetaDataRoleAuthorizationStrategy.authorize(realmDetail, ENABLE, entitlement);
        }
        form.add(realmDetail);

        final AjaxButton submit = getOnSubmit();
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new AjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = 530608535790823587L;

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

        add(form);
    }

    protected AjaxButton getOnSubmit() {
        return new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    closeAction(target, form);
                } catch (Exception e) {
                    LOG.error("While creating or updating user", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
    }

    protected void submitAction(final AjaxRequestTarget target, final Form<?> form) {
        final RealmTO updatedRealmTO = (RealmTO) form.getModelObject();
        realmRestClient.create(this.parentPath, updatedRealmTO);
    }

    protected void closeAction(final AjaxRequestTarget target, final Form<?> form) {
        this.window.close(target);
    }
}
