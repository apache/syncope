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
package org.apache.syncope.core.persistence.jpa;

import java.util.Collection;
import java.util.Optional;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;

public class DummyConnectorManager implements ConnectorManager {

    @Override
    public void registerConnector(final ExternalResource resource) throws NotFoundException {
    }

    @Override
    public void unregisterConnector(final String id) {
    }

    @Override
    public ConnInstance buildConnInstanceOverride(
            final ConnInstanceTO connInstance,
            final Collection<ConnConfProperty> confOverride,
            final Optional<Collection<ConnectorCapability>> capabilitiesOverride) {

        return null;
    }

    @Override
    public Connector createConnector(final ConnInstance connInstance) {
        return null;
    }

    @Override
    public Connector getConnector(final ExternalResource resource) {
        return null;
    }

    @Override
    public Optional<Connector> readConnector(final ExternalResource resource) {
        return Optional.empty();
    }

    @Override
    public void load() {
    }

    @Override
    public void unload() {
    }
}
