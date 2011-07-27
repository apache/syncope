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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.util.AttributableUtil;

@Transactional
public class DerSchemaTest extends AbstractTest {

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public final void findAll() {
        List<UDerSchema> list =
                derSchemaDAO.findAll(UDerSchema.class);
        assertEquals(2, list.size());
    }

    @Test
    public final void findByName() {
        UDerSchema attributeSchema =
                derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull("did not find expected derived attribute schema",
                attributeSchema);
    }

    @Test
    public final void save() {
        UDerSchema derivedAttributeSchema =
                new UDerSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("firstname surname");

        derSchemaDAO.save(derivedAttributeSchema);

        UDerSchema actual =
                derSchemaDAO.find("cn2", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public final void delete() {
        UDerSchema attributeSchema =
                derSchemaDAO.find("cn", UDerSchema.class);

        derSchemaDAO.delete(
                attributeSchema.getName(),
                AttributableUtil.USER);

        UDerSchema actual =
                derSchemaDAO.find("cn", UDerSchema.class);
        assertNull("delete did not work", actual);
    }
}
