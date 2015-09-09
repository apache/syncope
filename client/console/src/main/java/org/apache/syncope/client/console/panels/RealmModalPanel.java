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
package org.apache.syncope.client.console.panels;

import static org.apache.wicket.Component.ENABLE;

import com.googlecode.wicket.jquery.ui.markup.html.link.AjaxSubmitLink;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons.PrimaryModalButton;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmModalPanel extends AbstractModalPanel {

    private static final long serialVersionUID = -4285220460543213901L;

    protected RealmTO realmTO;

    private final PageReference pageRef;

    private final BaseModal<RealmTO> modal;

    @SpringBean
    private RealmRestClient realmRestClient;

    private final String parentPath;

    public RealmModalPanel(
            final String id,
            final PageReference pageRef,
            final BaseModal<RealmTO> modal,
            final RealmTO realmTO,
            final String parentPath,
            final String entitlement) {

        super(id);

        this.pageRef = pageRef;
        this.modal = modal;
        this.realmTO = realmTO;
        this.parentPath = parentPath;

        final Form<RealmTO> form = new Form<>("realmForm");
        form.setModel(new CompoundPropertyModel<>(realmTO));

        final RealmDetails realmDetail = new RealmDetails("details", realmTO);
        if (SyncopeConsoleSession.get().owns(entitlement)) {
            MetaDataRoleAuthorizationStrategy.authorize(realmDetail, ENABLE, entitlement);
        }
        form.add(realmDetail);

        final AjaxSubmitLink submit = getOnSubmit(form);
        modal.addFooterInput(submit);
        add(form);
    }

    protected final PrimaryModalButton getOnSubmit(final Form form) {
        return new PrimaryModalButton(BaseModal.getModalInputId(), "submit", form) {

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
                    LOG.error("While creating or updating realm", e);
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
        this.modal.close(target);
    }
}
