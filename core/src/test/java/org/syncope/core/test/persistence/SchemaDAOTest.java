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
package org.syncope.core.test.persistence;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.types.SchemaType;

@Transactional
public class SchemaDAOTest extends AbstractTest {

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public final void findAll() {
        List<UserSchema> userList = schemaDAO.findAll(UserSchema.class);
        assertEquals(7, userList.size());

        List<RoleSchema> roleList = schemaDAO.findAll(RoleSchema.class);
        assertEquals(2, roleList.size());
    }

    @Test
    public final void findByName() {
        UserSchema attributeSchema =
                schemaDAO.find("username", UserSchema.class);
        assertNotNull("did not find expected attribute schema",
                attributeSchema);
    }

    @Test
    public final void save() {
        UserSchema attributeSchema = new UserSchema();
        attributeSchema.setName("secondaryEmail");
        attributeSchema.setType(SchemaType.String);
        attributeSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatory(false);
        attributeSchema.setMultivalue(true);
        
        try {
            schemaDAO.save(attributeSchema);
        } catch (MultiUniqueValueException e) {
            log.error("Unexpected exception", e);
        }

        UserSchema actual = schemaDAO.find("secondaryEmail", UserSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(attributeSchema, actual);
    }

    @Test
    public final void delete() {
        UserSchema schema =
                schemaDAO.find("username", UserSchema.class);

        schemaDAO.delete(schema.getName(), UserSchema.class);

        UserSchema actual = schemaDAO.find("username", UserSchema.class);
        assertNull("delete did not work", actual);
    }
}
