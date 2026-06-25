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
package org.apache.syncope.core.persistence.jpa.openjpa;

import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.spring.implementation.ImplementationManager;

/**
 * Takes care of Implementation classes cache removal in case HA is set up and the actual change is performed by
 * another node in the Hibernate cluster.
 */
public class ImplementationManagerCacheEntryListener
        implements CacheEntryUpdatedListener<Object, Object>,
        CacheEntryRemovedListener<Object, Object>, Serializable {

    private static final long serialVersionUID = 5260753255454140460L;

    protected final EntityManagerFactory entityManagerFactory;

    protected final String domain;

    public ImplementationManagerCacheEntryListener(
            final EntityManagerFactory entityManagerFactory,
            final String domain) {

        this.entityManagerFactory = entityManagerFactory;
        this.domain = domain;
    }

    @Override
    public void onUpdated(final Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            String[] split = event.getKey().toString().split("#");
            if (split.length > 1) {
                if (JPAImplementation.class.getName().equals(split[0])) {
                    ImplementationManager.purge(domain, split[1]);
                }
            }
        }
    }

    @Override
    public void onRemoved(final Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            String[] split = event.getKey().toString().split("#");
            if (split.length > 1) {
                if (JPAImplementation.class.getName().equals(split[0])) {
                    ImplementationManager.purge(domain, split[1]);
                }
            }
        }
    }
}
