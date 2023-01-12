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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class SecurityQuestionITCase extends AbstractITCase {

    @Test
    public void read() {
        SecurityQuestionTO securityQuestionTO = SECURITY_QUESTION_SERVICE.read(
                "887028ea-66fc-41e7-b397-620d7ea6dfbb");
        assertNotNull(securityQuestionTO);
    }

    @Test
    public void list() {
        List<SecurityQuestionTO> securityQuestionTOs = SECURITY_QUESTION_SERVICE.list();
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

        Response response = SECURITY_QUESTION_SERVICE.create(securityQuestionTO);
        SecurityQuestionTO actual = getObject(response.getLocation(), SecurityQuestionService.class,
                SecurityQuestionTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getKey());
        securityQuestionTO.setKey(actual.getKey());
        assertEquals(actual, securityQuestionTO);
    }

    @Test
    public void update() {
        SecurityQuestionTO securityQuestionTO = SECURITY_QUESTION_SERVICE.read(
                "887028ea-66fc-41e7-b397-620d7ea6dfbb");
        securityQuestionTO.setContent("What is your favorite color?");

        SECURITY_QUESTION_SERVICE.update(securityQuestionTO);
        SecurityQuestionTO actual = SECURITY_QUESTION_SERVICE.read(securityQuestionTO.getKey());
        assertNotNull(actual);
        assertEquals(actual, securityQuestionTO);
    }

    @Test
    public void delete() {
        SecurityQuestionTO securityQuestion = new SecurityQuestionTO();
        securityQuestion.setContent("What is your first pet's name?");

        Response response = SECURITY_QUESTION_SERVICE.create(securityQuestion);
        securityQuestion = getObject(response.getLocation(), SecurityQuestionService.class, SecurityQuestionTO.class);

        SECURITY_QUESTION_SERVICE.delete(securityQuestion.getKey());

        try {
            SECURITY_QUESTION_SERVICE.read(securityQuestion.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

}
