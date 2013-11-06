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

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.console.rest.UserSelfRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with User form.
 */
public class UserSelfModalPage extends UserModalPage {

    private static final long serialVersionUID = 603212869211672852L;

    @SpringBean
    private UserSelfRestClient restClient;

    private final UserTO initialUserTO;

    public UserSelfModalPage(final PageReference callerPageRef, final ModalWindow window, final UserTO userTO) {
        super(callerPageRef, window, userTO, Mode.SELF, userTO.getId() != 0);

        this.initialUserTO = AttributableOperations.clone(userTO);
        setupEditPanel();
    }

    @Override
    protected void submitAction(final AjaxRequestTarget target, final Form form) {
        final UserTO updatedUserTO = (UserTO) form.getModelObject();

        if (updatedUserTO.getId() == 0) {
            restClient.create(updatedUserTO);
        } else {
            final UserMod userMod = AttributableOperations.diff(updatedUserTO, initialUserTO);

            // update user only if it has changed
            if (!userMod.isEmpty()) {
                restClient.update(userMod);
            }
        }
    }

    @Override
    protected void closeAction(final AjaxRequestTarget target, final Form form) {
        setResponsePage(new ResultStatusModalPage.Builder(window, userTO).mode(mode).build());
    }
}
