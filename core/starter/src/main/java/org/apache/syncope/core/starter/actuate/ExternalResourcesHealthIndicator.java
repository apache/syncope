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
package org.apache.syncope.core.starter.actuate;

import java.util.stream.Stream;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class ExternalResourcesHealthIndicator implements HealthIndicator {

    protected static final Logger LOG = LoggerFactory.getLogger(ExternalResourcesHealthIndicator.class);

    protected final DomainOps domainOps;

    protected final ExternalResourceDAO resourceDAO;

    protected final ConnInstanceDataBinder connInstanceDataBinder;

    protected final ConnectorManager connectorManager;

    public ExternalResourcesHealthIndicator(
            final DomainOps domainOps,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final ConnectorManager connectorManager) {

        this.domainOps = domainOps;
        this.resourceDAO = resourceDAO;
        this.connInstanceDataBinder = connInstanceDataBinder;
        this.connectorManager = connectorManager;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        Mutable<Boolean> anyDown = new MutableObject<>(false);

        Stream.concat(Stream.of(SyncopeConstants.MASTER_DOMAIN), domainOps.list().stream().map(Domain::getKey)).
                forEach(domain -> AuthContextUtils.runAsAdmin(domain, () -> {

            resourceDAO.findAll().forEach(resource -> {
                Status status;
                try {
                    connectorManager.createConnector(
                            connectorManager.buildConnInstanceOverride(
                                    connInstanceDataBinder.getConnInstanceTO(resource.getConnector()),
                                    resource.getConfOverride(),
                                    resource.getCapabilitiesOverride())).
                            test();
                    status = Status.UP;
                } catch (Exception e) {
                    status = Status.DOWN;
                    LOG.debug("When checking {} in Domain {}", resource.getKey(), domain, e);
                }

                builder.withDetail(domain + "#" + resource.getKey(), status);
                if (status != Status.UP) {
                    anyDown.setValue(true);
                }
            });
        }));

        builder.status(anyDown.getValue() ? Status.DOWN : Status.UP);

        return builder.build();
    }
}
