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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.services.SecurityQuestionService;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.common.types.ClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SecurityQuestionTestITCase extends AbstractTest {

    @Test
    public void read() {
        SecurityQuestionTO securityQuestionTO = securityQuestionService.read(1L);
        assertNotNull(securityQuestionTO);
    }

    @Test
    public void list() {
        List<SecurityQuestionTO> securityQuestionTOs = securityQuestionService.list();
        assertNotNull(securityQuestionTOs);
        assertFalse(securityQuestionTOs.isEmpty());
        for (SecurityQuestionTO instance : securityQuestionTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void create() {
        SecurityQuestionTO securityQuestionTO = new SecurityQuestionTO();
        securityQuestionTO.setContent("What is your favorite pet's name?");

        Response response = securityQuestionService.create(securityQuestionTO);
        SecurityQuestionTO actual = getObject(response.getLocation(), SecurityQuestionService.class,
                SecurityQuestionTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getId());
        securityQuestionTO.setId(actual.getId());
        assertEquals(actual, securityQuestionTO);
    }

    @Test
    public void update() {
        SecurityQuestionTO securityQuestionTO = securityQuestionService.read(1L);
        securityQuestionTO.setContent("What is your favorite color?");

        securityQuestionService.update(securityQuestionTO.getId(), securityQuestionTO);
        SecurityQuestionTO actual = securityQuestionService.read(securityQuestionTO.getId());
        assertNotNull(actual);
        assertEquals(actual, securityQuestionTO);
    }

    @Test
    public void delete() {
        SecurityQuestionTO securityQuestion = new SecurityQuestionTO();
        securityQuestion.setContent("What is your first pet's name?");

        Response response = securityQuestionService.create(securityQuestion);
        securityQuestion = getObject(response.getLocation(), SecurityQuestionService.class, SecurityQuestionTO.class);

        securityQuestionService.delete(securityQuestion.getId());

        try {
            securityQuestionService.read(securityQuestion.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

}
