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
package org.apache.syncope.client.enduser.pages;

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.wizards.any.UserWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Self extends BaseEnduserWebPage implements IEventSource {

    private static final long serialVersionUID = 164651008547631054L;

    private UserWizardBuilder userWizardBuilder;

    protected static final String WIZARD_ID = "wizard";

    public Self(final PageParameters parameters) {
        super(parameters);

        body.add(buildWizard(SyncopeEnduserSession.get().isAuthenticated()
                ? SyncopeEnduserSession.get().getSelfTO()
                : buildNewUserTO(),
                SyncopeEnduserSession.get().isAuthenticated()
                ? AjaxWizard.Mode.EDIT
                : AjaxWizard.Mode.CREATE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            final AjaxWizard.NewItemEvent<AnyWrapper<UserTO>> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.
                    getPayload());
            final AjaxRequestTarget target = newItemEvent.getTarget();
            final AnyWrapper<UserTO> item = newItemEvent.getItem();
            if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                SyncopeEnduserSession.get().invalidate();
                setResponsePage(Login.class);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeEnduserSession.get().invalidate();
                setResponsePage(LandingPage.class);
            }
        }
        super.onEvent(event);
    }

    protected final AjaxWizard<AnyWrapper<UserTO>> buildWizard(final UserTO userTO, final AjaxWizard.Mode mode) {
        userWizardBuilder = new UserWizardBuilder(
                null,
                userTO,
                SyncopeEnduserSession.get().getService(SyncopeService.class).platform().getUserClasses(),
                new UserFormLayoutInfo(),
                this.getPageReference());
        userWizardBuilder.setItem(new UserWrapper(userTO));
        return userWizardBuilder.build(WIZARD_ID, mode);
    }

    private UserTO buildNewUserTO() {
        final UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        return userTO;
    }

}
