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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.AuthenticationChainDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationChain;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AuthenticationChainTest extends AbstractTest {

    @Autowired
    private AuthenticationChainDAO authenticationChainDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void find() {
        AuthenticationChain authenticationChain = authenticationChainDAO.find(
                "4735ce66-aa3f-416b-b810-9b2c1d25ada7");
        assertNotNull(authenticationChain);

        authenticationChain = authenticationChainDAO.find(UUID.randomUUID().toString());
        assertNull(authenticationChain);
    }

    @Test
    public void findAll() {
        List<AuthenticationChain> authenticationChains = authenticationChainDAO.findAll();
        assertNotNull(authenticationChains);
        assertEquals(1, authenticationChains.size());
    }

    @Test
    public void save() {
        Implementation authenticationChainRule = entityFactory.newEntity(Implementation.class);
        authenticationChainRule.setKey(UUID.randomUUID().toString());
        authenticationChainRule.setEngine(ImplementationEngine.JAVA);
        authenticationChainRule.setType(AMImplementationType.AUTH_CHAIN_RULES);
        authenticationChainRule.setBody(POJOHelper.serialize(""));

        int beforeCount = authenticationChainDAO.findAll().size();

        authenticationChainRule = implementationDAO.save(authenticationChainRule);

        assertNotNull(authenticationChainRule);
        assertNotNull(authenticationChainRule.getKey());

        AuthenticationChain authenticationChain = entityFactory.newEntity(AuthenticationChain.class);
        authenticationChain.setName("AuthenticationChainTest");
        authenticationChain.add(authenticationChainRule);
        authenticationChainDAO.save(authenticationChain);

        assertNotNull(authenticationChain);
        assertNotNull(authenticationChain.getKey());

        int afterCount = authenticationChainDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void delete() {
        AuthenticationChain authenticationChain = authenticationChainDAO.find(
                "4735ce66-aa3f-416b-b810-9b2c1d25ada7");
        assertNotNull(authenticationChain);

        authenticationChainDAO.delete("4735ce66-aa3f-416b-b810-9b2c1d25ada7");

        authenticationChain = authenticationChainDAO.find("4735ce66-aa3f-416b-b810-9b2c1d25ada7");
        assertNull(authenticationChain);
    }

}
