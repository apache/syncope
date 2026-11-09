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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecurityQuestionDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private SecurityQuestionDataBinder binder;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Test
    void get() {
        SecurityQuestionTO securityQuestion = binder.getSecurityQuestionTO(
                securityQuestionDAO.findById("887028ea-66fc-41e7-b397-620d7ea6dfbb").orElseThrow());
        assertEquals("887028ea-66fc-41e7-b397-620d7ea6dfbb", securityQuestion.getKey());
        assertEquals("What's your mother's maiden name?", securityQuestion.getContent());
    }

    @Test
    void createUpdate() {
        SecurityQuestionTO securityQuestionTO = new SecurityQuestionTO();
        securityQuestionTO.setContent("What is your favourite colour?");
        SecurityQuestion securityQuestion = securityQuestionDAO.save(binder.create(securityQuestionTO));
        assertNotNull(securityQuestion.getKey());

        SecurityQuestionTO created = binder.getSecurityQuestionTO(securityQuestion);
        assertEquals("What is your favourite colour?", created.getContent());

        created.setContent("What is your favourite animal?");
        binder.update(securityQuestion, created);

        assertEquals("What is your favourite animal?", binder.getSecurityQuestionTO(securityQuestion).getContent());
    }
}
