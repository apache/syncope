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

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.pages.panels.AccountInformationPanel;
import org.apache.syncope.console.pages.panels.StatusPanel;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.util.AttributableOperations;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal window with User form.
 */
public class EditUserModalPage extends UserModalPage {

    @SpringBean
    private UserRestClient userRestClient;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(EditUserModalPage.class);

    private UserTO initialUserTO = null;

    public EditUserModalPage(final PageReference callerPageRef, final ModalWindow window, final UserTO userTO) {

        super(callerPageRef, window, userTO, Mode.ADMIN, true);

        this.initialUserTO = AttributableOperations.clone(userTO);

        Form form = setupEditPanel();

        // add resource assignment details in case of update
        if (userTO.getId() != 0) {
            final List<StatusBean> statuses = new ArrayList<StatusBean>();

            form.addOrReplace(new StatusPanel("statuspanel", userTO, statuses, false));

            form.addOrReplace(new AccountInformationPanel("accountinformation", userTO));
        }
    }

    public EditUserModalPage(final ModalWindow window, final UserTO userTO) {
        super(window, userTO, Mode.ADMIN);
    }

    @Override
    protected void submitAction(final AjaxRequestTarget target, final Form form) {

        final UserTO updatedUserTO = (UserTO) form.getModelObject();

        if (updatedUserTO.getId() == 0) {
            try {
                userTO = userRestClient.create(updatedUserTO);
            } catch (Exception e) {
                LOG.error("Could not update user. \n {}", e.getMessage());
            }
        } else {
            final UserMod userMod = AttributableOperations.diff(updatedUserTO, initialUserTO);

            // update user just if it is changed
            if (!userMod.isEmpty()) {
                try {
                    userTO = userRestClient.update(userMod);
                } catch (Exception e) {
                    LOG.error("Could not create User. \n {}", e.getMessage());
                }
            }
        }

    }

    @Override
    protected void closeAction(final AjaxRequestTarget target, final Form form) {
        setResponsePage(new EditUserModalPage(window, userTO));
    }
}
