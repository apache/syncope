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
package org.apache.syncope.core.logic.saml2;

import java.util.Optional;
import org.pac4j.core.context.session.SessionStore;

public final class NoOpSessionStore implements SessionStore<SAML2SP4UIContext> {

    public static final NoOpSessionStore INSTANCE = new NoOpSessionStore();

    private NoOpSessionStore() {
        // private constructor for singleton
    }

    @Override
    public String getOrCreateSessionId(final SAML2SP4UIContext context) {
        return "<NO_KEY>";
    }

    @Override
    public Optional<Object> get(final SAML2SP4UIContext context, final String key) {
        return Optional.empty();
    }

    @Override
    public void set(final SAML2SP4UIContext context, final String key, final Object value) {
        // nothing to do
    }

    @Override
    public boolean destroySession(final SAML2SP4UIContext context) {
        return true;
    }

    @Override
    public Optional<?> getTrackableSession(final SAML2SP4UIContext context) {
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore<SAML2SP4UIContext>> buildFromTrackableSession(
            final SAML2SP4UIContext context,
            final Object trackableSession) {

        return Optional.empty();
    }

    @Override
    public boolean renewSession(final SAML2SP4UIContext context) {
        return false;
    }
}
