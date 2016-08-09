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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class SecurityQuestionTest extends AbstractTest {

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Test
    public void find() {
        SecurityQuestion securityQuestion = securityQuestionDAO.find(
                "887028ea-66fc-41e7-b397-620d7ea6dfbb");
        assertNotNull(securityQuestion);
        assertNotNull(securityQuestion.getContent());
    }

    @Test
    public void findAll() {
        List<SecurityQuestion> securityQuestions = securityQuestionDAO.findAll();
        assertNotNull(securityQuestions);
        assertFalse(securityQuestions.isEmpty());
    }

    @Test
    public void save() {
        SecurityQuestion securityQuestion = entityFactory.newEntity(SecurityQuestion.class);
        securityQuestion.setContent("What is your favorite pet's name?");

        SecurityQuestion actual = securityQuestionDAO.save(securityQuestion);
        assertNotNull(actual);
        assertNotNull(actual.getKey());
    }

    @Test
    public void delete() {
        securityQuestionDAO.delete("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        assertNull(securityQuestionDAO.find("887028ea-66fc-41e7-b397-620d7ea6dfbb"));
    }
}
