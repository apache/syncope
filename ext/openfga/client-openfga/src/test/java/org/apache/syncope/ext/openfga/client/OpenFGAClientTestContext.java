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
package org.apache.syncope.ext.openfga.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:core-test.properties")
@Import(OpenFGAClientContext.class)
@Configuration(proxyBeanMethods = false)
public class OpenFGAClientTestContext {

    public static final String NEW_TYPE_KEY = "NEWTYPE";

    @Bean
    public DomainHolder<?> domainHolder() {
        return new DomainHolder<Object>() {

            @Override
            public Map<String, Object> getDomains() {
                return Map.of(SyncopeConstants.MASTER_DOMAIN, new Object());
            }

            @Override
            public Map<String, Boolean> getHealthInfo() {
                return Map.of(SyncopeConstants.MASTER_DOMAIN, true);
            }
        };
    }

    @Bean
    public RelationshipTypeDAO relationshipTypeDAO() {
        RelationshipTypeDAO relationshipTypeDAO = mock(RelationshipTypeDAO.class);
        when(relationshipTypeDAO.findByLeftEndAnyType(any(AnyType.class))).
                thenAnswer(ic -> List.of(OpenFGAClientTest.mockRelationshipType()));
        return relationshipTypeDAO;
    }
}
