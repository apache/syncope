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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ConfTest extends AbstractTest {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    private void add(final CPlainAttr newAttr, final String value) {
        JPACPlainAttrValue attrValue;
        if (newAttr.getSchema().isUniqueConstraint()) {
            attrValue = new JPACPlainAttrValue();
            ((PlainAttrUniqueValue) attrValue).setSchema(newAttr.getSchema());
        } else {
            attrValue = new JPACPlainAttrValue();
        }
        newAttr.add(value, attrValue);
    }

    @Test
    public void update() {
        CPlainAttr expireTime = confDAO.find("token.expireTime").get();
        assertNotNull(expireTime);
        long value = expireTime.getValues().get(0).getLongValue();
        value++;

        CPlainAttr attr = entityFactory.newEntity(CPlainAttr.class);
        attr.setSchema(plainSchemaDAO.find("token.expireTime"));
        add(attr, String.valueOf(value));

        confDAO.save(expireTime);
        confDAO.flush();

        CPlainAttr actual = confDAO.find("token.expireTime").get();
        assertEquals(expireTime, actual);
    }

}
