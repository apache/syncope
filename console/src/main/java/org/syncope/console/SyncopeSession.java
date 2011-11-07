/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.syncope.console.commons.Constants;

/**
 * Custom Syncope Session class.
 */
public class SyncopeSession extends WebSession {

    private String userId;

    private String coreVersion;

    private Roles roles = new Roles();

    public static SyncopeSession get() {
        return (SyncopeSession) Session.get();
    }

    public SyncopeSession(final Request request) {
        super(request);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getCoreVersion() {
        return coreVersion;
    }

    public void setCoreVersion(String coreVersion) {
        this.coreVersion = coreVersion;
    }

    public void setEntitlements(final String[] entitlements) {
        roles = new Roles(entitlements);
    }

    public boolean isAuthenticated() {
        return !roles.isEmpty();
    }

    public boolean hasAnyRole(final Roles roles) {
        return this.roles.hasAnyRole(roles);
    }

    public DateFormat getDateFormat() {
        String language = "en";
        if (getLocale() != null) {
            language = getLocale().getLanguage();
        }

        DateFormat formatter;
        if ("it".equals(language)) {
            formatter = new SimpleDateFormat(Constants.ITALIAN_DATE_FORMAT);
        } else {
            formatter = new SimpleDateFormat(Constants.ENGLISH_DATE_FORMAT);
        }

        return formatter;
    }
}
