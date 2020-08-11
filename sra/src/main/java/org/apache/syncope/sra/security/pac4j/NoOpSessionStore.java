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
package org.apache.syncope.sra.security.pac4j;

import java.util.Optional;
import org.pac4j.core.context.session.SessionStore;

public final class NoOpSessionStore implements SessionStore<ServerWebExchangeContext> {

    public static final NoOpSessionStore INSTANCE = new NoOpSessionStore();

    private NoOpSessionStore() {
        // private constructor for singleton
    }

    @Override
    public String getOrCreateSessionId(final ServerWebExchangeContext context) {
        return "<NO_KEY>";
    }

    @Override
    public Optional<Object> get(final ServerWebExchangeContext context, final String key) {
        return Optional.empty();
    }

    @Override
    public void set(final ServerWebExchangeContext context, final String key, final Object value) {
        // nothing to do
    }

    @Override
    public boolean destroySession(final ServerWebExchangeContext context) {
        return true;
    }

    @Override
    public Optional<?> getTrackableSession(final ServerWebExchangeContext context) {
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore<ServerWebExchangeContext>> buildFromTrackableSession(
            final ServerWebExchangeContext context,
            final Object trackableSession) {

        return Optional.empty();
    }

    @Override
    public boolean renewSession(final ServerWebExchangeContext context) {
        return false;
    }
}
