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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCacheSessionStore implements SessionStore {

    protected static final Logger LOG = LoggerFactory.getLogger(JCacheSessionStore.class);

    protected final Cache<String, Map<String, Object>> cache;

    protected final EncryptorManager encryptorManager;

    public JCacheSessionStore(
            final Cache<String, Map<String, Object>> cache,
            final EncryptorManager encryptorManager) {

        this.cache = cache;
        this.encryptorManager = encryptorManager;
    }

    @Override
    public Optional<String> getSessionId(final WebContext webContext, final boolean createSession) {
        return webContext instanceof OIDCC4UIContext ctx
                ? Optional.ofNullable(ctx.getId()).or(() -> {
                    if (createSession) {
                        String id = SecureRandomUtils.generateRandomUUID().toString();
                        ctx.setId(id);
                        cache.put(id, new HashMap<>());
                        return Optional.of(id);
                    }
                    return Optional.empty();
                })
                : Optional.empty();
    }

    @Override
    public Optional<Object> get(final WebContext webContext, final String key) {
        return webContext instanceof OIDCC4UIContext ctx
                ? Optional.ofNullable(ctx.getId()).
                        or(() -> getSessionId(webContext, true)).
                        map(cache::get).
                        flatMap(session -> Optional.ofNullable(session.get(key))).
                        map(value -> {
                            try {
                                return encryptorManager.getInstance().
                                        decode((String) value, CipherAlgorithm.AES);
                            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                                    | IllegalBlockSizeException | BadPaddingException e) {

                                LOG.error("While getting {} from JCache with id {}", key, ctx.getId());
                                return StringUtils.EMPTY;
                            }
                        })
                : Optional.empty();
    }

    @Override
    public void set(final WebContext webContext, final String key, final Object value) {
        if (webContext instanceof OIDCC4UIContext ctx) {
            Optional.ofNullable(ctx.getId()).
                    or(() -> getSessionId(webContext, true)).
                    ifPresent(id -> cache.invoke(id, (entry, args) -> {
                        if (!entry.exists()) {
                            entry.setValue(new HashMap<>());
                        }
                        if (value == null) {
                            entry.getValue().remove(key);
                        } else {
                            try {
                                entry.getValue().put(
                                        key,
                                        encryptorManager.getInstance().encode(value.toString(), CipherAlgorithm.AES));
                            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                                    | IllegalBlockSizeException | BadPaddingException e) {

                                LOG.error("While setting {}={} from JCache with id {}", key, value, id);
                            }
                        }
                        return entry.getValue();
                    }));
        }
    }

    @Override
    public boolean destroySession(final WebContext webContext) {
        if (webContext instanceof OIDCC4UIContext ctx) {
            Optional.ofNullable(ctx.getId()).ifPresent(cache::remove);
            return true;
        }
        return false;
    }

    @Override
    public Optional<Object> getTrackableSession(final WebContext webContext) {
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore> buildFromTrackableSession(
            final WebContext webContext,
            final Object trackableSession) {

        return Optional.empty();
    }

    @Override
    public boolean renewSession(final WebContext webContext) {
        return false;
    }
}
