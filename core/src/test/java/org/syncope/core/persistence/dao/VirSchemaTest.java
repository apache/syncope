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
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.util.AttributableUtil;

@Transactional
public class VirSchemaTest extends AbstractTest {

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Test
    public final void findAll() {
        List<UVirSchema> list =
                virSchemaDAO.findAll(UVirSchema.class);
        assertEquals(1, list.size());
    }

    @Test
    public final void findByName() {
        UVirSchema attributeSchema =
                virSchemaDAO.find("virtualdata", UVirSchema.class);
        assertNotNull("did not find expected virtual attribute schema",
                attributeSchema);
    }

    @Test
    public final void save() {
        UVirSchema virtualAttributeSchema =
                new UVirSchema();
        virtualAttributeSchema.setName("virtual");

        virSchemaDAO.save(virtualAttributeSchema);

        UVirSchema actual =
                virSchemaDAO.find("virtual", UVirSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(virtualAttributeSchema, actual);
    }

    @Test
    public final void delete() {
        UVirSchema attributeSchema =
                virSchemaDAO.find("virtualdata", UVirSchema.class);

        virSchemaDAO.delete(
                attributeSchema.getName(),
                AttributableUtil.USER);

        UVirSchema actual =
                virSchemaDAO.find("virtualdata", UVirSchema.class);
        assertNull("delete did not work", actual);
    }
}
