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
package org.apache.syncope.console.pages.panels;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.SyncopeSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

public class AccountInformationPanel extends SysInfoPanel {

    private static final long serialVersionUID = 4228064224811390808L;

    public <T extends AbstractAttributableTO> AccountInformationPanel(final String id, final UserTO userTO) {
        super(id, userTO);

        // ------------------------
        // Change password date
        // ------------------------
        add(new Label("changePwdDate", new Model<String>(userTO.getChangePwdDate() == null
                ? StringUtils.EMPTY
                : SyncopeSession.get().getDateFormat().format(userTO.getChangePwdDate()))));
        // ------------------------

        // ------------------------
        // Last login date
        // ------------------------
        add(new Label("lastLoginDate", new Model<String>(userTO.getLastLoginDate() == null
                ? StringUtils.EMPTY
                : SyncopeSession.get().getDateFormat().format(userTO.getLastLoginDate()))));
        // ------------------------

        // ------------------------
        // Failed logins
        // ------------------------
        add(new Label("failedLogins", new Model<Integer>(userTO.getFailedLogins())));
        // ------------------------

        // ------------------------
        // Token
        // ------------------------
        add(new Label("token", new Model<String>(userTO.getToken() == null
                ? StringUtils.EMPTY
                : userTO.getToken())));
        // ------------------------

        // ------------------------
        // Token expire time
        // ------------------------
        add(new Label("tokenExpireTime", new Model<String>(userTO.getTokenExpireTime() == null
                ? StringUtils.EMPTY
                : SyncopeSession.get().getDateFormat().format(userTO.getTokenExpireTime()))));
        // ------------------------
    }
}
