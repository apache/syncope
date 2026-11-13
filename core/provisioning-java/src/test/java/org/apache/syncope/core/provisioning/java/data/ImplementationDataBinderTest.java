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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ImplementationDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private ImplementationDataBinder binder;

    @Autowired
    private ImplementationDAO implementationDAO;

    @BeforeAll
    public static void populateImplementationTypes() {
        ImplementationTypesHolder.getInstance().putAll(IdRepoImplementationType.values());
    }

    @Test
    void get() {
        ImplementationTO implementation = binder.getImplementationTO(
                implementationDAO.findById("SampleReportJobDelegate").orElseThrow());
        assertEquals("SampleReportJobDelegate", implementation.getKey());
        assertEquals(ImplementationEngine.JAVA, implementation.getEngine());
        assertEquals(IdRepoImplementationType.REPORT_DELEGATE, implementation.getType());
        assertEquals(implementation.getBody(), implementationDAO.findById("SampleReportJobDelegate").
                orElseThrow().getBody());
    }

    @Test
    void createUpdate() {
        DefaultAccountRuleConf conf = new DefaultAccountRuleConf();
        conf.setMaxLength(32);
        conf.setMinLength(8);

        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey("testAccountRule");
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(IdRepoImplementationType.ACCOUNT_RULE);
        implementationTO.setBody(POJOHelper.serialize(conf));

        Implementation implementation = implementationDAO.save(binder.create(implementationTO));

        ImplementationTO created = binder.getImplementationTO(implementation);
        assertEquals("testAccountRule", created.getKey());
        assertEquals(ImplementationEngine.JAVA, created.getEngine());
        assertEquals(IdRepoImplementationType.ACCOUNT_RULE, created.getType());
        assertEquals(implementationTO.getBody(), created.getBody());

        conf.setMaxLength(64);
        created.setBody(POJOHelper.serialize(conf));
        binder.update(implementation, created);
        assertEquals(created.getBody(), binder.getImplementationTO(implementation).getBody());
    }

    @Test
    void invalid() {
        ImplementationTO blankBody = new ImplementationTO();
        blankBody.setKey("testImplementation");
        blankBody.setEngine(ImplementationEngine.JAVA);
        blankBody.setType(IdRepoImplementationType.ACCOUNT_RULE);
        assertThrows(SyncopeClientException.class, () -> binder.create(blankBody));

        ImplementationTO invalidType = new ImplementationTO();
        invalidType.setKey("testImplementation");
        invalidType.setEngine(ImplementationEngine.JAVA);
        invalidType.setType("invalidType");
        invalidType.setBody("{}");
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidType));

        ImplementationTO invalidBody = new ImplementationTO();
        invalidBody.setKey("testImplementation");
        invalidBody.setEngine(ImplementationEngine.JAVA);
        invalidBody.setType(IdRepoImplementationType.ACCOUNT_RULE);
        invalidBody.setBody("{}");
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidBody));

        Implementation existing = implementationDAO.findById("SampleReportJobDelegate").orElseThrow();
        ImplementationTO changedType = binder.getImplementationTO(existing);
        changedType.setType(IdRepoImplementationType.ACCOUNT_RULE);
        assertThrows(SyncopeClientException.class, () -> binder.update(existing, changedType));
    }
}
