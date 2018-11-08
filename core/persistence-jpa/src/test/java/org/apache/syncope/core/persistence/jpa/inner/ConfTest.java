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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ConfTest extends AbstractTest {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void read() {
        Optional<? extends CPlainAttr> conf = confDAO.find("selfRegistration.allowed");
        assertTrue(conf.isPresent());
        assertTrue(conf.get().getValues().get(0).getBooleanValue());

        conf = confDAO.find("authentication.statuses");
        assertTrue(conf.isPresent());
        assertEquals(2, conf.get().getValues().size());

        conf = confDAO.find("non.existing");
        assertFalse(conf.isPresent());
    }

    private void add(final CPlainAttr newAttr, final String value) {
        PlainAttrValue attrValue;
        if (newAttr.getSchema().isUniqueConstraint()) {
            attrValue = entityFactory.newEntity(CPlainAttrUniqueValue.class);
            ((PlainAttrUniqueValue) attrValue).setSchema(newAttr.getSchema());
        } else {
            attrValue = entityFactory.newEntity(CPlainAttrValue.class);
        }
        newAttr.add(value, attrValue);
    }

    @Test
    public void setAndDelete() {
        // 1. create CSChema
        PlainSchema useless = entityFactory.newEntity(PlainSchema.class);
        useless.setKey("useless");
        useless.setType(AttrSchemaType.Date);
        useless.setConversionPattern("yyyy-MM-dd");
        useless = plainSchemaDAO.save(useless);

        // 2. create conf
        CPlainAttr newConf = entityFactory.newEntity(CPlainAttr.class);
        newConf.setOwner(confDAO.get());
        newConf.setSchema(useless);
        add(newConf, "2014-06-20");
        confDAO.save(newConf);

        Optional<? extends CPlainAttr> actual = confDAO.find("useless");
        assertEquals(actual.get().getValuesAsStrings(), newConf.getValuesAsStrings());

        // 3. update conf
        newConf.getValues().clear();
        add(newConf, "2014-06-20");
        confDAO.save(newConf);

        actual = confDAO.find("useless");
        assertEquals(actual.get().getValuesAsStrings(), newConf.getValuesAsStrings());

        // 4. delete conf
        confDAO.delete("useless");
        plainSchemaDAO.delete("useless");
        assertFalse(confDAO.find("useless").isPresent());
    }

    @Test
    public void issueSYNCOPE418() {
        try {
            PlainSchema failing = entityFactory.newEntity(PlainSchema.class);
            failing.setKey("http://schemas.examples.org/security/authorization/organizationUnit");
            failing.setType(AttrSchemaType.String);
            plainSchemaDAO.save(failing);

            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidKey));
        }
    }
}
