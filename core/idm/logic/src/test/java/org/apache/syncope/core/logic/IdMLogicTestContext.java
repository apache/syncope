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
package org.apache.syncope.core.logic;

import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.jpa.PersistenceContext;
import org.apache.syncope.core.provisioning.java.ProvisioningContext;
import org.apache.syncope.core.spring.security.SecurityContext;
import org.apache.syncope.core.workflow.java.WorkflowContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:core-test.properties")
@Import({ SecurityContext.class, PersistenceContext.class, ProvisioningContext.class, WorkflowContext.class })
@ComponentScan("org.apache.syncope.core.logic")
@Configuration
public class IdMLogicTestContext {

    @Primary
    @Bean
    public ImplementationLookup implementationLookup() {
        return new DummyImplementationLookup();
    }
}
