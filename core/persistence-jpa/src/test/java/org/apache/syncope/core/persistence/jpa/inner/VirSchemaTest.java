/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class VirSchemaTest extends AbstractTest {

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Test
    public void findAll() {
        List<VirSchema> list = virSchemaDAO.findAll();
        assertEquals(3, list.size());
    }

    @Test
    public void findByName() {
        VirSchema attributeSchema = virSchemaDAO.find("virtualdata");
        assertNotNull("did not find expected virtual attribute schema", attributeSchema);
    }

    @Test
    public void save() {
        ExternalResource csv = resourceDAO.find("resource-csv");
        Provision provision = csv.getProvision(ObjectClass.ACCOUNT).get();
        assertNotNull(provision);

        VirSchema virSchema = entityFactory.newEntity(VirSchema.class);
        virSchema.setKey("virtual");
        virSchema.setProvision(provision);
        virSchema.setReadonly(true);
        virSchema.setExtAttrName("EXT_ATTR");

        virSchemaDAO.save(virSchema);

        VirSchema actual = virSchemaDAO.find("virtual");
        assertNotNull("expected save to work", actual);
        assertTrue(actual.isReadonly());
        assertEquals("EXT_ATTR", actual.getExtAttrName());
    }

    @Test
    public void delete() {
        VirSchema virtualdata = virSchemaDAO.find("virtualdata");

        virSchemaDAO.delete(virtualdata.getKey());

        VirSchema actual = virSchemaDAO.find("virtualdata");
        assertNull("delete did not work", actual);

        // ------------- //
        VirSchema rvirtualdata = virSchemaDAO.find("rvirtualdata");
        assertNotNull(rvirtualdata);

        virSchemaDAO.delete(rvirtualdata.getKey());

        actual = virSchemaDAO.find("rvirtualdata");
        assertNull("delete did not work", actual);
    }

    @Test
    public void issueSYNCOPE418() {
        VirSchema schema = entityFactory.newEntity(VirSchema.class);
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            virSchemaDAO.save(schema);
            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
