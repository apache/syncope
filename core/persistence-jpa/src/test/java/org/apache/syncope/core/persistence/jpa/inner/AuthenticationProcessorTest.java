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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.AuthenticationPolicyRule;
import org.apache.syncope.core.persistence.api.dao.AuthenticationProcessorDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPostProcessor;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPreProcessor;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationProcessor;

@Transactional("Master")
public class AuthenticationProcessorTest extends AbstractTest {

    @Autowired
    private AuthenticationProcessorDAO authenticationProcessorDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findAll() {
        List<AuthenticationProcessor> processors = authenticationProcessorDAO.findAll();
        assertNotNull(processors);
        assertFalse(processors.isEmpty());
    }

    @Test
    public void findByKey() {
        AuthenticationProcessor authPostProcessor =
                authenticationProcessorDAO.find("2460e430-ce67-41a5-86ed-ea0a4e78c0a3");
        assertNotNull(authPostProcessor);

        AuthenticationProcessor authPreProcessor =
                authenticationProcessorDAO.find("c413566e-8859-11e9-bc42-526af7764f64");
        assertNotNull(authPreProcessor);
    }

    @Test
    public void findByType() {
        List<AuthenticationPostProcessor> processors = authenticationProcessorDAO.
                find(AuthenticationPostProcessor.class);
        assertNotNull(processors);
        assertFalse(processors.isEmpty());
    }

    @Test
    public void create() {

        AuthenticationPostProcessor authenticationPostProcessor =
                entityFactory.newEntity(AuthenticationPostProcessor.class);
        authenticationPostProcessor.setDefaultFailureLoginURL("login/error");
        authenticationPostProcessor.setDefaultSuccessLoginURL("login");

        Implementation postProcessing = entityFactory.newEntity(Implementation.class);
        postProcessing.setKey("PostProcessingTest");
        postProcessing.setEngine(ImplementationEngine.JAVA);
        postProcessing.setType(AMImplementationType.AUTH_POST_PROCESSING);
        postProcessing.setBody(AuthenticationPolicyRule.class.getName());
        postProcessing = implementationDAO.save(postProcessing);
        authenticationPostProcessor.addAuthPostProcessing(postProcessing);

        AuthenticationPreProcessor authenticationPreProcessor =
                entityFactory.newEntity(AuthenticationPreProcessor.class);

        Implementation preProcessing = entityFactory.newEntity(Implementation.class);
        preProcessing.setKey("PreProcessingTest");
        preProcessing.setEngine(ImplementationEngine.JAVA);
        preProcessing.setType(AMImplementationType.AUTH_PRE_PROCESSING);
        preProcessing.setBody(AuthenticationPolicyRule.class.getName());
        preProcessing = implementationDAO.save(preProcessing);
        authenticationPreProcessor.addAuthPreProcessing(preProcessing);
    }

    @Test
    public void update() {
        AuthenticationPostProcessor authPostProcessor =
                authenticationProcessorDAO.find("2460e430-ce67-41a5-86ed-ea0a4e78c0a3");
        assertNotNull(authPostProcessor);
        assertEquals(1, authPostProcessor.getAuthenticationPostProcessing().size());
        assertEquals("login", authPostProcessor.getDefaultSuccessLoginURL());

        authPostProcessor.setDefaultSuccessLoginURL("login/home");
        authPostProcessor = authenticationProcessorDAO.save(authPostProcessor);

        assertNotNull(authPostProcessor);
        assertEquals("login/home", authPostProcessor.getDefaultSuccessLoginURL());
    }

    @Test
    public void delete() {
        AuthenticationPostProcessor authPostProcessor =
                authenticationProcessorDAO.find("2460e430-ce67-41a5-86ed-ea0a4e78c0a3");
        assertNotNull(authPostProcessor);

        authenticationProcessorDAO.delete(authPostProcessor);

        authPostProcessor = authenticationProcessorDAO.find("2460e430-ce67-41a5-86ed-ea0a4e78c0a3");
        assertNull(authPostProcessor);
    }
}
