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

import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserRequestFormDetails extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -8847854414429745216L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    public UserRequestFormDetails(final PageReference pageRef, final UserRequestForm formTO) {
        super(MultilevelPanel.SECOND_LEVEL_ID);

        final UserTO newUserTO;
        final UserTO previousUserTO;
        if (formTO.getUserUR() == null) {
            newUserTO = formTO.getUserTO();
            previousUserTO = null;
        } else if (formTO.getUserTO() == null) {
            // make it stronger by handling possible NPE
            previousUserTO = new UserTO();
            previousUserTO.setKey(formTO.getUserUR().getKey());
            newUserTO = AnyOperations.patch(previousUserTO, formTO.getUserUR());
        } else {
            formTO.getUserTO().setKey(formTO.getUserUR().getKey());
            newUserTO = AnyOperations.patch(formTO.getUserTO(), formTO.getUserUR());
            previousUserTO = formTO.getUserTO();
        }

        add(new UserWizardBuilder(
                previousUserTO,
                newUserTO,
                anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses(),
                new UserFormLayoutInfo(),
                userRestClient,
                pageRef).
                build(AjaxWizard.Mode.READONLY));
    }
}
