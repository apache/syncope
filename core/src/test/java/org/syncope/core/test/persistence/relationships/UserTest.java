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
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.test.persistence.AbstractTest;

@Transactional
public class UserTest extends AbstractTest {

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

    @Test
    public final void test() {
        syncopeUserDAO.delete(4L);

        syncopeUserDAO.flush();

        assertNull(syncopeUserDAO.find(4L));
        assertNull(attributeDAO.find(550L, UserAttribute.class));
        assertNull(attributeValueDAO.find(22L, UserAttributeValue.class));
        assertNotNull(schemaDAO.find("loginDate", UserSchema.class));
        assertTrue(syncopeRoleDAO.find(7L).getUsers().isEmpty());
    }
}
