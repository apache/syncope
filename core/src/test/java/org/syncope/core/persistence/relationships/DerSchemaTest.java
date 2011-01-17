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
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.DerAttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.AbstractTest;

@Transactional
public class DerSchemaTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Test
    public final void test() {
        derSchemaDAO.delete("cn", UDerSchema.class);

        derSchemaDAO.flush();

        assertNull(derSchemaDAO.find("cn", UDerSchema.class));
        assertNull(derAttrDAO.find(1000L, UDerAttr.class));
        assertTrue(schemaDAO.find("surname",
                USchema.class).getDerivedSchemas().isEmpty());
        assertTrue(schemaDAO.find("firstname",
                USchema.class).getDerivedSchemas().isEmpty());
        assertNull(userDAO.find(3L).getDerivedAttribute("cn"));
    }
}
