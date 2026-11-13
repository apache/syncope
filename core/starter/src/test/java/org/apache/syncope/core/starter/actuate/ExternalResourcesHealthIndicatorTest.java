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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class ExternalResourcesHealthIndicatorTest {

    private static ExternalResource resource(final String key) {
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn(key);
        when(resource.getConnector()).thenReturn(mock(ConnInstance.class));
        when(resource.getConfOverride()).thenReturn(Optional.empty());
        when(resource.getCapabilitiesOverride()).thenReturn(Optional.empty());
        return resource;
    }

    private static Connector connectorFor(final boolean healthy) {
        Connector connector = mock(Connector.class);
        if (!healthy) {
            doThrow(new IllegalStateException("connection failed")).when(connector).test();
        }
        return connector;
    }

    private Health health(final List<Domain> domains, final ExternalResourceDAO resourceDAO, final boolean healthy) {
        DomainOps domainOps = mock(DomainOps.class);
        when(domainOps.list()).thenReturn(domains);

        ConnectorManager connectorManager = mock(ConnectorManager.class);
        ConnInstance connInstance = mock(ConnInstance.class);
        Connector connector = connectorFor(healthy);
        when(connectorManager.buildConnInstanceOverride(any(), any(), any())).thenReturn(connInstance);
        when(connectorManager.createConnector(connInstance)).thenReturn(connector);

        try (MockedStatic<AuthContextUtils> authContextUtils = mockStatic(AuthContextUtils.class)) {
            authContextUtils.when(() -> AuthContextUtils.runAsAdmin(anyString(), any(Runnable.class))).
                    thenAnswer(invocation -> {
                        invocation.getArgument(1, Runnable.class).run();
                        return null;
                    });

            return new ExternalResourcesHealthIndicator(domainOps, resourceDAO, connectorManager).health();
        }
    }

    @Test
    void isUpWhenAllResourcesAreHealthy() {
        ExternalResourceDAO resourceDAO = mock(ExternalResourceDAO.class);
        doReturn(List.of(resource("res1"), resource("res2"))).when(resourceDAO).findAll();

        Health result = health(List.of(), resourceDAO, true);

        assertEquals(Status.UP, result.getStatus());
        assertEquals(
                Set.of("Master#res1", "Master#res2"),
                result.getDetails().keySet());
        assertEquals(Status.UP, result.getDetails().get("Master#res1"));
        assertEquals(Status.UP, result.getDetails().get("Master#res2"));
    }

    @Test
    void isDownWhenAnyResourceIsUnhealthy() {
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("Other");

        ExternalResourceDAO resourceDAO = mock(ExternalResourceDAO.class);
        doReturn(List.of(resource("res1"))).when(resourceDAO).findAll();

        Health result = health(List.of(domain), resourceDAO, false);

        assertEquals(Status.DOWN, result.getStatus());
        assertEquals(Map.of("Master#res1", Status.DOWN, "Other#res1", Status.DOWN), result.getDetails());
    }

    @Test
    void isUpWhenNoResourceIsConfigured() {
        ExternalResourceDAO resourceDAO = mock(ExternalResourceDAO.class);
        when(resourceDAO.findAll()).thenReturn(List.of());

        assertEquals(Status.UP, health(List.of(), resourceDAO, true).getStatus());
    }
}
