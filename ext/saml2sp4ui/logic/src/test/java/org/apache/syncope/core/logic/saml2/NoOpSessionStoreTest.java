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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.pac4j.core.context.WebContext;

class NoOpSessionStoreTest {

    private final NoOpSessionStore store = NoOpSessionStore.INSTANCE;

    @Test
    void neverStoresAnything() {
        WebContext context = mock(WebContext.class);

        assertTrue(store.getSessionId(context, true).isEmpty());
        assertTrue(store.get(context, "key").isEmpty());
        assertTrue(store.getTrackableSession(context).isEmpty());
        assertTrue(store.buildFromTrackableSession(context, new Object()).isEmpty());

        store.set(context, "key", "value");
        assertTrue(store.get(context, "key").isEmpty());

        assertTrue(store.destroySession(context));
        assertFalse(store.renewSession(context));
    }
}
