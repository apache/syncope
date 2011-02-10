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

import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebSession;

/**
 * Custom Syncope Session class.
 */
public class SyncopeSession extends WebSession {

    private String userId;

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

    public void setEntitlements(final String[] entitlements) {
        roles = new Roles(entitlements);
    }

    public boolean isAuthenticated() {
        return !roles.isEmpty();
    }

    public boolean hasAnyRole(final Roles roles) {
        return this.roles.hasAnyRole(roles);
    }
}
