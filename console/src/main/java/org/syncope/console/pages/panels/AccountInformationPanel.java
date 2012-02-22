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
package org.syncope.console.pages.panels;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.SyncopeSession;

public class AccountInformationPanel extends Panel {

    public <T extends AbstractAttributableTO> AccountInformationPanel(
            final String id,
            final UserTO userTO) {

        super(id);

        // ------------------------
        // Creation date
        // ------------------------
        add(new Label("creationDate", new Model(
                userTO.getCreationDate() != null
                ? SyncopeSession.get().getDateFormat().format(
                userTO.getCreationDate()) : "")));
        // ------------------------

        // ------------------------
        // Change password date
        // ------------------------
        add(new Label("changePwdDate", new Model(
                userTO.getChangePwdDate() != null
                ? SyncopeSession.get().getDateFormat().format(
                userTO.getChangePwdDate()) : "")));
        // ------------------------

        // ------------------------
        // Last login date
        // ------------------------
        add(new Label("lastLoginDate", new Model(
                userTO.getLastLoginDate() != null
                ? SyncopeSession.get().getDateFormat().format(
                userTO.getLastLoginDate()) : "")));
        // ------------------------

        // ------------------------
        // Failed logins
        // ------------------------
        add(new Label("failedLogins", new Model(userTO.getFailedLogins() != null
                ? userTO.getFailedLogins() : "")));
        // ------------------------

        // ------------------------
        // Token
        // ------------------------
        add(new Label("token", new Model(
                userTO.getToken() != null ? userTO.getToken() : "")));
        // ------------------------

        // ------------------------
        // Token expire time
        // ------------------------
        add(new Label("tokenExpireTime", new Model(
                userTO.getTokenExpireTime() != null
                ? userTO.getTokenExpireTime() : "")));
        // ------------------------
    }
}
