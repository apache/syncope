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
package org.apache.syncope.wa.starter;

import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.authentication.attribute.AttributeDefinition;
import org.apereo.cas.authentication.attribute.AttributeDefinitionResolutionContext;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStore;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.IPersonAttributeDaoFilter;
import org.apereo.services.persondir.IPersonAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = { SyncopeWAApplication.class, AbstractTest.SyncopeTestConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "cas.authn.accept.users=mrossi::password",
            "cas.authn.syncope.url=http://localhost:8080",
            "cas.sso.services.allow-missing-service-parameter=true",
            "cas.authn.pac4j.core.name=DelegatedClientAuthenticationHandler"
        })
@TestPropertySource(locations = { "classpath:wa.properties", "classpath:test.properties" })
@ContextConfiguration(initializers = ZookeeperTestingServer.class)
public abstract class AbstractTest {

    private static class DummyIPersonAttributeDao implements IPersonAttributeDao {

        @Override
        public IPersonAttributes getPerson(
                final String string,
                final Set<IPersonAttributes> set,
                final IPersonAttributeDaoFilter ipadf) {

            return null;
        }

        @Override
        public Set<IPersonAttributes> getPeople(
                final Map<String, Object> map,
                final IPersonAttributeDaoFilter ipadf,
                final Set<IPersonAttributes> set) {

            return Set.of();
        }

        @Override
        public Map<String, Object> getTags() {
            return Map.of();
        }

        @Override
        public int compareTo(final IPersonAttributeDao o) {
            return 0;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof IPersonAttributeDao;
        }
    }

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @TestConfiguration
    @ComponentScan("org.apache.syncope.wa.starter")
    public static class SyncopeTestConfiguration {

        @Bean
        public SyncopeCoreTestingServer syncopeCoreTestingServer() {
            return new SyncopeCoreTestingServer();
        }

        // The following bean definitions allow for MacOS builds to complete successfully
        @Bean(name = AttributeDefinitionStore.BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AttributeDefinitionStore attributeDefinitionStore() {
            return new AttributeDefinitionStore() {

                @Override
                public AttributeDefinitionStore registerAttributeDefinition(final AttributeDefinition defn) {
                    return this;
                }

                @Override
                public AttributeDefinitionStore registerAttributeDefinition(
                        final String key, final AttributeDefinition defn) {

                    return this;
                }

                @Override
                public Optional<AttributeDefinition> locateAttributeDefinitionByName(final String name) {
                    return Optional.empty();
                }

                @Override
                public AttributeDefinitionStore removeAttributeDefinition(final String key) {
                    return this;
                }

                @Override
                public Optional<AttributeDefinition> locateAttributeDefinition(final String key) {
                    return Optional.empty();
                }

                @Override
                public <T extends AttributeDefinition> Optional<T> locateAttributeDefinition(
                        final String key, final Class<T> clazz) {

                    return Optional.empty();
                }

                @Override
                public <T extends AttributeDefinition> Optional<T> locateAttributeDefinition(
                        final Predicate<AttributeDefinition> predicate) {

                    return Optional.empty();
                }

                @Override
                public Collection<AttributeDefinition> getAttributeDefinitions() {
                    return Set.of();
                }

                @Override
                public <T extends AttributeDefinition> Stream<T> getAttributeDefinitionsBy(final Class<T> type) {
                    return Stream.empty();
                }

                @Override
                public Optional<Pair<AttributeDefinition, List<Object>>> resolveAttributeValues(
                        final String key, final AttributeDefinitionResolutionContext context) {

                    return Optional.empty();
                }

                @Override
                public Map<String, List<Object>> resolveAttributeValues(
                        final Collection<String> attributeDefinitions,
                        final Map<String, List<Object>> availableAttributes,
                        final Principal principal,
                        final RegisteredService registeredService,
                        final Service service) {

                    return Map.of();
                }

                @Override
                public boolean isEmpty() {
                    return true;
                }

                @Override
                public AttributeDefinitionStore store(final Resource resource) {
                    return this;
                }

                @Override
                public AttributeDefinitionStore importStore(final AttributeDefinitionStore definitionStore) {
                    return this;
                }
            };
        }

        @Bean(name = { "cachingAttributeRepository", PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY })
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public IPersonAttributeDao cachingAttributeRepository() {
            return new DummyIPersonAttributeDao();
        }

        @Bean(name = "aggregatingAttributeRepository")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public IPersonAttributeDao aggregatingAttributeRepository() {
            return new DummyIPersonAttributeDao();
        }
    }

    @LocalServerPort
    protected int port;

    @Autowired
    private WARestClient waRestClient;

    @BeforeEach
    public void waitForCore() {
        await().atMost(50, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> waRestClient.isReady());
    }
}
