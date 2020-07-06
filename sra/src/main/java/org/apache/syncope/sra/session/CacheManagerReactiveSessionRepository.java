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
package org.apache.syncope.sra.session;

import org.apache.syncope.sra.SessionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.session.MapSession;
import org.springframework.session.ReactiveSessionRepository;
import reactor.core.publisher.Mono;

public class CacheManagerReactiveSessionRepository implements ReactiveSessionRepository<MapSession> {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Mono<MapSession> createSession() {
        return Mono.just(new MapSession());
    }

    @Override
    public Mono<Void> save(final MapSession session) {
        return Mono.fromRunnable(() -> {
            if (!session.getId().equals(session.getOriginalId())) {
                cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(session.getOriginalId());
            }
            cacheManager.getCache(SessionConfig.DEFAULT_CACHE).put(session.getId(), session);
        });
    }

    @Override
    public Mono<MapSession> findById(final String id) {
        return Mono.defer(() -> Mono.justOrEmpty(
                cacheManager.getCache(SessionConfig.DEFAULT_CACHE).get(id, MapSession.class)).
                map(MapSession::new).
                switchIfEmpty(deleteById(id).then(Mono.empty())));
    }

    @Override
    public Mono<Void> deleteById(final String id) {
        return Mono.fromRunnable(() -> cacheManager.getCache(SessionConfig.DEFAULT_CACHE).evictIfPresent(id));
    }
}
