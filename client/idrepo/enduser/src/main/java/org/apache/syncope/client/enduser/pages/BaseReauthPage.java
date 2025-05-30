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

import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

abstract class BaseReauthPage extends BasePage {

    private static final long serialVersionUID = 417902520012512819L;

    protected BaseReauthPage(final PageParameters parameters, final String name) {
        super(parameters, name);

        if (isReauthExpired()) {
            LOG.debug("Re-authentication needed to proceed");

            PageParameters notification = new PageParameters();
            notification.add(Constants.NOTIFICATION_MSG_PARAM, getString("reauth.message"));
            notification.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.WARNING);

            throw new RestartResponseAtInterceptPageException(ReauthLogin.class, notification);
        } else {
            SyncopeEnduserSession.get().clearLastReauth();
            LOG.debug("Re-authentication cleared");
        }
    }

    protected boolean isReauthExpired() {
        return SyncopeEnduserSession.get().isReauthExpired();
    }
}
