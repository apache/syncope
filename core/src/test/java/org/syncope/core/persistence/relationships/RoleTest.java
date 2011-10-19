/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RAttrValue;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.PolicyDAO;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private AttrValueDAO attrValueDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private PolicyDAO policyDAO;

    public final void createWithPasswordPolicy() {
        final String ROLE_NAME = "roleWithPasswordPolicy";

        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        SyncopeRole role = new SyncopeRole();
        role.setName(ROLE_NAME);
        role.setPasswordPolicy(policy);

        SyncopeRole actual = roleDAO.save(role);
        assertNotNull(actual);

        actual = roleDAO.find(actual.getId());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        roleDAO.delete(actual.getId());
        assertNull(roleDAO.find(actual.getId()));

        assertNotNull(policyDAO.find(4L));
    }

    @Test
    public final void delete() {
        roleDAO.delete(2L);

        roleDAO.flush();

        assertNull(roleDAO.find(2L));
        assertEquals(1, entitlementDAO.find("base").getRoles().size());
        assertEquals(userDAO.find(2L).getRoles().size(), 2);
        assertNull(attrDAO.find(700L, RAttr.class));
        assertNull(attrValueDAO.find(41L, RAttrValue.class));
        assertNotNull(schemaDAO.find("icon", RSchema.class));
    }
}
