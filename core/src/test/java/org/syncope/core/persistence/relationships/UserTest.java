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

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.membership.Membership;

@Transactional
public class UserTest extends AbstractTest {

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

    @Test
    public final void test() {
        userDAO.delete(4L);

        userDAO.flush();

        assertNull(userDAO.find(4L));
        assertNull(attrDAO.find(550L, UAttr.class));
        assertNull(attrValueDAO.find(22L, UAttrValue.class));
        assertNotNull(schemaDAO.find("loginDate", USchema.class));

        List<Membership> memberships =
                roleDAO.findMemberships(roleDAO.find(7L));
        assertTrue(memberships.isEmpty());
    }
}
