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

import java.io.Serializable;
import java.util.Collection;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.event.RemoteCommitListener;
import org.apache.openjpa.util.StringId;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.spring.implementation.ImplementationManager;

/**
 * Takes care of connectors' Spring beans (un)registration in case HA is set up and the actual change is performed by
 * another node in the OpenJPA cluster.
 */
public class ImplementationManagerRemoteCommitListener implements RemoteCommitListener, Serializable {

    private static final long serialVersionUID = 5260753255454140460L;

    protected final String domain;

    public ImplementationManagerRemoteCommitListener(final String domain) {
        this.domain = domain;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterCommit(final RemoteCommitEvent event) {
        if (event.getPayloadType() != RemoteCommitEvent.PAYLOAD_EXTENTS) {
            ((Collection<Object>) event.getUpdatedObjectIds()).stream().
                    filter(StringId.class::isInstance).
                    map(StringId.class::cast).
                    forEach(id -> {
                        if (JPAImplementation.class.isAssignableFrom(id.getType())) {
                            ImplementationManager.purge(domain, id.getId());
                        }
                    });

            ((Collection<Object>) event.getDeletedObjectIds()).stream().
                    filter(StringId.class::isInstance).
                    map(StringId.class::cast).
                    forEach(id -> {
                        if (JPAImplementation.class.isAssignableFrom(id.getType())) {
                            ImplementationManager.purge(domain, id.getId());
                        }
                    });
        }
    }

    @Override
    public void close() {
        // nothing to do
    }
}
