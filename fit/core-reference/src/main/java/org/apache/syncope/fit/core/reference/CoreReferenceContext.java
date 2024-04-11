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
package org.apache.syncope.fit.core.reference;

import com.nimbusds.jose.JOSEException;
import org.apache.syncope.core.logic.IdRepoLogicContext;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.AuditEventProcessor;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@AutoConfigureBefore(IdRepoLogicContext.class)
@ComponentScan("org.apache.syncope.fit.core.reference")
@Configuration(proxyBeanMethods = false)
public class CoreReferenceContext {

    @ConditionalOnClass(name = "org.apache.syncope.core.flowable.impl.FlowableUserWorkflowAdapter")
    @Bean
    public EnableFlowableForTestUsers enableFlowableForTestUsers(final UserDAO userDAO) {
        return new EnableFlowableForTestUsers(userDAO);
    }

    @Bean
    public ImplementationLookup implementationLookup(
            final DomainHolder<?> domainHolder,
            final UserWorkflowAdapter uwf,
            final ObjectProvider<EnableFlowableForTestUsers> enableFlowableForTestUsers) {

        return new ITImplementationLookup(domainHolder, uwf, enableFlowableForTestUsers);
    }

    @Bean
    public AuditEventProcessor testFileAuditProcessor(final AuditEventDAO auditEventDAO) {
        return new TestFileAuditProcessor(auditEventDAO);
    }

    @Bean
    public CustomJWTSSOProvider customJWTSSOProvider(
            final AnySearchDAO anySearchDAO,
            final @Lazy AuthDataAccessor authDataAccessor) throws JOSEException {

        return new CustomJWTSSOProvider(anySearchDAO, authDataAccessor);
    }
}
