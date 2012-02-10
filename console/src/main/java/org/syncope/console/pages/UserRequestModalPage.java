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

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.console.rest.UserRequestRestClient;
import org.syncope.console.rest.UserRestClient;

/**
 * Modal window with User form.
 */
public class UserRequestModalPage extends UserModalPage {

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private UserRequestRestClient requestRestClient;

    private UserTO initialUserTO;

    private UserRequestTO userRequestTO;

    public UserRequestModalPage(
            final PageReference callerPageRef,
            final ModalWindow window,
            final UserTO userTO) {

        super(callerPageRef, window, userTO, Mode.SELF, false);

        setupEditPanel();
    }

    public UserRequestModalPage(
            final PageReference callerPageRef,
            final ModalWindow window,
            final UserRequestTO userRequestTO) {

        super(callerPageRef, window, null, Mode.SELF, false);

        // evaluate userTO ...
        switch (userRequestTO.getType()) {
            case CREATE:
                userTO = userRequestTO.getUserTO();
                this.initialUserTO = AttributableOperations.clone(userTO);
                break;

            case UPDATE:
                this.initialUserTO =
                        userRestClient.read(userRequestTO.getUserMod().getId());

                userTO = AttributableOperations.apply(
                        initialUserTO,
                        userRequestTO.getUserMod());
                break;

            case DELETE:
            default:
        }

        this.userRequestTO = userRequestTO;

        setupEditPanel();
    }

    public UserRequestModalPage(
            final ModalWindow window,
            final UserTO userTO) {

        super(window, userTO, Mode.SELF);
    }

    @Override
    protected void submitAction(
            final AjaxRequestTarget target, final Form form) {
        final UserTO updatedUserTO = (UserTO) form.getModelObject();

        if (updatedUserTO.getId() == 0) {
            requestRestClient.requestCreate(updatedUserTO);
        } else {
            final UserMod userMod = AttributableOperations.diff(
                    updatedUserTO, userRestClient.read(updatedUserTO.getId()));

            // update user just if it is changed
            if (!userMod.isEmpty()) {
                requestRestClient.requestUpdate(userMod);
            }
        }
    }

    @Override
    protected void closeAction(
            final AjaxRequestTarget target, final Form form) {
        setResponsePage(new UserRequestModalPage(window, userTO));
    }
}
