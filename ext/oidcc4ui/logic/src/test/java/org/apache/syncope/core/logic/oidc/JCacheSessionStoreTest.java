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
package org.apache.syncope.core.logic.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.InvalidKeyException;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.WebContext;

class JCacheSessionStoreTest {

    private CacheManager cacheManager;

    private Cache<String, Map<String, Object>> cache;

    private Encryptor encryptor;

    private JCacheSessionStore store;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        cacheManager = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider").
                getCacheManager();
        MutableConfiguration<String, Map<String, Object>> configuration =
                new MutableConfiguration<String, Map<String, Object>>().
                        setTypes(String.class, (Class<Map<String, Object>>) (Class<?>) Map.class).
                        setStoreByValue(false).
                        setReadThrough(false);
        cache = cacheManager.createCache("sessionStore", configuration);

        encryptor = mock(Encryptor.class);
        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        store = new JCacheSessionStore(cache, encryptorManager);
    }

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cacheManager.destroyCache(cache.getName());
        }
        cacheManager.close();
    }

    private void encryptsAndDecrypts() throws Exception {
        when(encryptor.encode(anyString(), eq(CipherAlgorithm.AES))).
                thenAnswer(invocation -> "ENC(" + invocation.getArgument(0) + ')');
        when(encryptor.decode(anyString(), eq(CipherAlgorithm.AES))).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            return value.substring("ENC(".length(), value.length() - 1);
        });
    }

    @Test
    void createSessionOnlyWhenRequested() {
        OIDCC4UIContext context = new OIDCC4UIContext();

        assertTrue(store.getSessionId(context, false).isEmpty());

        String id = store.getSessionId(context, true).orElseThrow();
        assertEquals(id, context.getId());
        assertNotNull(cache.get(id));
    }

    @Test
    void setAndGet() throws Exception {
        encryptsAndDecrypts();

        OIDCC4UIContext context = new OIDCC4UIContext();
        store.set(context, "key", "value");

        assertEquals("ENC(value)", cache.get(context.getId()).get("key"));
        assertEquals("value", store.get(context, "key").orElseThrow());
    }

    @Test
    void nullValueRemovesKey() throws Exception {
        encryptsAndDecrypts();

        OIDCC4UIContext context = new OIDCC4UIContext();
        store.set(context, "key", "value");
        store.set(context, "key", null);

        assertTrue(store.get(context, "key").isEmpty());
    }

    @Test
    void decryptFailureReturnsEmptyString() throws Exception {
        encryptsAndDecrypts();

        OIDCC4UIContext context = new OIDCC4UIContext();
        store.set(context, "key", "value");

        when(encryptor.decode(anyString(), eq(CipherAlgorithm.AES))).thenThrow(new InvalidKeyException());

        assertEquals("", store.get(context, "key").orElseThrow());
    }

    @Test
    void destroySession() throws Exception {
        encryptsAndDecrypts();

        OIDCC4UIContext context = new OIDCC4UIContext();
        store.set(context, "key", "value");
        String id = context.getId();

        assertTrue(store.destroySession(context));
        assertFalse(cache.containsKey(id));
        assertTrue(store.get(context, "key").isEmpty());
    }

    @Test
    void nonOIDCContextIsIgnored() throws Exception {
        encryptsAndDecrypts();

        WebContext context = mock(WebContext.class);

        assertTrue(store.getSessionId(context, true).isEmpty());
        assertTrue(store.get(context, "key").isEmpty());
        assertFalse(store.destroySession(context));

        store.set(context, "key", "value");
        assertFalse(cache.iterator().hasNext());
    }

    @Test
    void trackableSessionsAreNotSupported() {
        WebContext context = mock(WebContext.class);

        assertTrue(store.getTrackableSession(context).isEmpty());
        assertTrue(store.buildFromTrackableSession(context, new Object()).isEmpty());
        assertFalse(store.renewSession(context));
    }

    @Test
    void sessionIsSharedAcrossStoreInstances() throws Exception {
        encryptsAndDecrypts();

        OIDCC4UIContext context = new OIDCC4UIContext();
        store.set(context, "key", "value");

        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);
        JCacheSessionStore other = new JCacheSessionStore(cache, encryptorManager);

        assertEquals("value", other.get(context, "key").orElseThrow());
    }
}
