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
package org.syncope.core.test.persistence.relationships;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.RoleAttribute;
import org.syncope.core.persistence.beans.role.RoleAttributeValue;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.test.persistence.AbstractTest;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private AttributeValueDAO attributeValueDAO;
    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public final void test() {
        syncopeRoleDAO.delete(2L);

        syncopeRoleDAO.getEntityManager().flush();

        assertNull(syncopeRoleDAO.find(2L));
        assertTrue(entitlementDAO.find("base").getRoles().size() == 1);
        assertTrue(syncopeUserDAO.find(2L).getRoles().size() == 1);
        assertNull(attributeDAO.find(700L, RoleAttribute.class));
        assertNull(attributeValueDAO.find(41L, RoleAttributeValue.class));
        assertNotNull(schemaDAO.find("icon", RoleSchema.class));
    }
}
