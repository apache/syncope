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
package org.apache.syncope.client.console.wizards.any;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.annotations.UserFormFinalize;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;

@UserFormFinalize(mode = AjaxWizard.Mode.EDIT_APPROVAL)
public class UserRequestFormFinalizer implements UserFormFinalizer {

    protected final UserRequestRestClient userRequestRestClient;

    public UserRequestFormFinalizer(final UserRequestRestClient userRequestRestClient) {
        this.userRequestRestClient = userRequestRestClient;
    }

    @Override
    public void afterUpdate(final String userKey) {
        userRequestRestClient.getForm(userKey).ifPresent(form -> {
            try {
                userRequestRestClient.claimForm(form.getTaskId());
            } catch (SyncopeClientException e) {
                SyncopeConsoleSession.get().onException(e);
            }
        });
    }
}
