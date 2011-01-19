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
import org.springframework.test.annotation.ExpectedException;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.util.AttributableUtil;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.SchemaType;

@Transactional
public class SchemaTest extends AbstractTest {

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public final void findAll() {
        List<USchema> userList = schemaDAO.findAll(USchema.class);
        assertEquals(10, userList.size());

        List<RSchema> roleList = schemaDAO.findAll(RSchema.class);
        assertEquals(2, roleList.size());
    }

    @Test
    public final void findByName() {
        USchema attributeSchema =
                schemaDAO.find("username", USchema.class);
        assertNotNull("did not find expected attribute schema",
                attributeSchema);
    }

    @Test
    public final void getAttributes() {
        List<RSchema> schemas = schemaDAO.findAll(RSchema.class);
        assertNotNull(schemas);
        assertFalse(schemas.isEmpty());

        List<RAttr> attrs;
        for (RSchema schema : schemas) {
            attrs = schemaDAO.getAttributes(schema, RAttr.class);
            assertNotNull(attrs);
            assertFalse(attrs.isEmpty());
        }
    }

    @Test
    public final void save() {
        USchema attributeSchema = new USchema();
        attributeSchema.setName("secondaryEmail");
        attributeSchema.setType(SchemaType.String);
        attributeSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatoryCondition("false");
        attributeSchema.setMultivalue(true);

        schemaDAO.save(attributeSchema);

        USchema actual = schemaDAO.find("secondaryEmail", USchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(attributeSchema, actual);
    }

    @Test
    @ExpectedException(InvalidEntityException.class)
    public final void saveNonValid() {
        USchema attributeSchema = new USchema();
        attributeSchema.setName("secondaryEmail");
        attributeSchema.setType(SchemaType.String);
        attributeSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatoryCondition("false");
        attributeSchema.setMultivalue(true);
        attributeSchema.setUniqueConstraint(true);

        schemaDAO.save(attributeSchema);
    }

    @Test
    public final void delete() {
        USchema schema = schemaDAO.find("username", USchema.class);

        schemaDAO.delete(schema.getName(), AttributableUtil.USER);

        USchema actual = schemaDAO.find("username", USchema.class);
        assertNull("delete did not work", actual);
    }
}
