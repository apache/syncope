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

import org.apache.syncope.console.rest.UserRequestRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.to.UserRequestTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.util.AttributableOperations;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with User form.
 */
public class UserRequestModalPage extends UserModalPage {

    private static final long serialVersionUID = 603212869211672852L;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private UserRequestRestClient requestRestClient;

    private UserTO initialUserTO;

    private UserRequestTO userRequestTO;

    public UserRequestModalPage(final PageReference callerPageRef, final ModalWindow window, final UserTO userTO,
            final Mode mode) {

        super(callerPageRef, window, userTO, mode, false);

        setupEditPanel();
    }

    public UserRequestModalPage(final PageReference callerPageRef, final ModalWindow window,
            final UserRequestTO userRequestTO, final Mode mode) {

        super(callerPageRef, window, null, mode, false);

        switch (userRequestTO.getType()) {
            case CREATE:
                userTO = userRequestTO.getUserTO();
                this.initialUserTO = AttributableOperations.clone(userTO);
                break;

            case UPDATE:
                this.initialUserTO = userRestClient.read(userRequestTO.getUserMod().getId());

                userTO = AttributableOperations.apply(initialUserTO, userRequestTO.getUserMod());
                break;

            case DELETE:
            default:
        }

        this.userRequestTO = userRequestTO;

        setupEditPanel();
    }

    public UserRequestModalPage(final ModalWindow window, final UserTO userTO, final Mode mode) {

        super(window, userTO, mode);
    }

    @Override
    protected void submitAction(final AjaxRequestTarget target, final Form form) {
        final UserTO updatedUserTO = (UserTO) form.getModelObject();

        if (updatedUserTO.getId() == 0) {
            switch (mode) {
                case SELF:
                    requestRestClient.requestCreate(updatedUserTO);
                    break;

                case ADMIN:
                    userRestClient.create(updatedUserTO);
                    if (userRequestTO != null) {
                        requestRestClient.delete(userRequestTO.getId());
                    }
                    break;

                default:
                    LOG.warn("Invalid mode specified for {}: {}", getClass().getName(), mode);
            }
        } else {
            final UserMod userMod = AttributableOperations.diff(updatedUserTO,
                    userRestClient.read(updatedUserTO.getId()));

            // update user only if it has changed
            if (!userMod.isEmpty()) {
                switch (mode) {
                    case SELF:
                        requestRestClient.requestUpdate(userMod);
                        break;

                    case ADMIN:
                        userRestClient.update(userMod);
                        if (userRequestTO != null) {
                            requestRestClient.delete(userRequestTO.getId());
                        }
                        break;

                    default:
                        LOG.warn("Invalid mode specified for {}: {}", getClass().getName(), mode);
                }
            }
        }
    }

    @Override
    protected void closeAction(final AjaxRequestTarget target, final Form form) {
        setResponsePage(new UserRequestModalPage(window, userTO, mode));
    }
}
