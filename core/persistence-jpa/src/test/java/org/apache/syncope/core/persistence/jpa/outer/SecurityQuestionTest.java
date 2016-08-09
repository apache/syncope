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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertNull;

import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class SecurityQuestionTest extends AbstractTest {

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void test() {
        User user = userDAO.findByUsername("bellini");
        assertNull(user.getSecurityQuestion());
        assertNull(user.getSecurityAnswer());

        user.setSecurityQuestion(securityQuestionDAO.find("887028ea-66fc-41e7-b397-620d7ea6dfbb"));
        user.setSecurityAnswer("Rossi");
        userDAO.save(user);

        userDAO.flush();

        securityQuestionDAO.delete("887028ea-66fc-41e7-b397-620d7ea6dfbb");

        userDAO.flush();

        user = userDAO.findByUsername("bellini");

        assertNull(user.getSecurityQuestion());
        assertNull(user.getSecurityAnswer());
    }
}
