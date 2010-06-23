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
package org.syncope.core.test.rest;

import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.client.to.DerivedSchemaTO;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.to.SchemaTO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.AttributeType;
import static org.junit.Assert.*;

public class SchemaTestITCase extends AbstractTestITCase {

    @Autowired
    SchemaDAO schemaDAO;
    @Autowired
    DerivedSchemaDAO derivedSchemaDAO;

    @Test
    public void attributeList() {
        List<SchemaTO> userSchemas =
                restTemplate.getForObject(BASE_URL
                + "schema/user/list.json", List.class);
        assertFalse(userSchemas.isEmpty());

        List<SchemaTO> roleSchemas = restTemplate.getForObject(BASE_URL
                + "schema/role/list.json", List.class);
        assertFalse(roleSchemas.isEmpty());
    }

    @Test
    public void attributeCreate() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("testAttribute");
        schemaTO.setMandatory(true);
        schemaTO.setType(AttributeType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);
    }

    @Test
    public void derivedAttributeList() {
        List<DerivedSchemaTO> derivedSchemas =
                restTemplate.getForObject(BASE_URL
                + "derivedSchema/user/list.json", List.class);
        assertFalse(derivedSchemas.isEmpty());
    }
}
