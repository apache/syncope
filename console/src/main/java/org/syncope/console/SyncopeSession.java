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
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.Roles;

/**
 * Custom Syncope Session class.
 */
public class SyncopeSession extends AuthenticatedWebSession {

    private SyncopeUser user;

    public static SyncopeSession get() {
        return (SyncopeSession) Session.get();
    }

    public SyncopeSession(Request request) {
        super(request);

        setLocale(request.getLocale());
    }

    public synchronized SyncopeUser getUser() {
        return user;
    }

    public synchronized boolean isAuthenticated() {
        return (user != null);
    }

    public synchronized void setUser(SyncopeUser user) {
        this.user = user;
        dirty();
    }

    /*
     * Requested by AuthenticatedWebSession, but actually
     * not used (replaced in Login page).
     */
    @Override
    public boolean authenticate(final String username, final String password) {
        return ((SyncopeSession) Session.get()).getUser() != null;
    }

    @Override
    public Roles getRoles() {
        return getUser().getRoles();
    }
}
