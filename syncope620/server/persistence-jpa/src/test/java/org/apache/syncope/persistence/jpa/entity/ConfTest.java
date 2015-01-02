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
package org.apache.syncope.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.persistence.api.dao.ConfDAO;
import org.apache.syncope.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConfTest extends AbstractTest {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Test
    public void read() {
        CPlainAttr conf = confDAO.find("selfRegistration.allowed");
        assertNotNull(conf);
        assertTrue(conf.getValues().get(0).getBooleanValue());

        conf = confDAO.find("authentication.statuses");
        assertNotNull(conf);
        assertEquals(2, conf.getValues().size());

        conf = confDAO.find("non.existing");
        assertNull(conf);
    }

    @Test
    public void setAndDelete() {
        // 1. create CSChema
        CPlainSchema useless = entityFactory.newEntity(CPlainSchema.class);
        useless.setKey("useless");
        useless.setType(AttrSchemaType.Date);
        useless.setConversionPattern("yyyy-MM-dd");
        useless = plainSchemaDAO.save(useless);

        // 2. create conf
        CPlainAttr newConf = entityFactory.newEntity(CPlainAttr.class);
        newConf.setSchema(useless);
        newConf.addValue("2014-06-20", JPAAttributableUtil.getInstance(AttributableType.CONFIGURATION));
        confDAO.save(newConf);

        CPlainAttr actual = confDAO.find("useless");
        assertEquals(actual.getValuesAsStrings(), newConf.getValuesAsStrings());

        // 3. update conf
        newConf.getValues().clear();
        newConf.addValue("2014-06-20", JPAAttributableUtil.getInstance(AttributableType.CONFIGURATION));
        confDAO.save(newConf);

        actual = confDAO.find("useless");
        assertEquals(actual.getValuesAsStrings(), newConf.getValuesAsStrings());

        // 4. delete conf
        confDAO.delete("useless");
        assertNull(confDAO.find("useless"));
    }

    @Test
    public void issueSYNCOPE418() {
        try {
            CPlainSchema failing = entityFactory.newEntity(CPlainSchema.class);
            failing.setKey("http://schemas.examples.org/security/authorization/organizationUnit");
            failing.setType(AttrSchemaType.String);
            plainSchemaDAO.save(failing);

            fail();
        } catch (InvalidEntityException e) {
            assertTrue(e.hasViolation(EntityViolationType.InvalidName));
        }
    }
}
