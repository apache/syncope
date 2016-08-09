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
package org.apache.syncope.core.provisioning.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class MappingTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void connObjectLink() {
        ExternalResource ldap = resourceDAO.find("resource-ldap");
        assertNotNull(ldap);

        Provision provision = ldap.getProvision(anyTypeDAO.findUser());
        assertNotNull(provision);
        assertNotNull(provision.getMapping());
        assertNotNull(provision.getMapping().getConnObjectLink());

        User user = userDAO.findByUsername("rossini");
        assertNotNull(user);

        Name name = MappingUtils.evaluateNAME(user, provision, user.getUsername());
        assertEquals("uid=rossini,ou=people,o=isp", name.getNameValue());

        provision.getMapping().setConnObjectLink("'uid=' + username + ',o=' + realm + ',ou=people,o=isp'");

        name = MappingUtils.evaluateNAME(user, provision, user.getUsername());
        assertEquals("uid=rossini,o=even,ou=people,o=isp", name.getNameValue());
    }
}
