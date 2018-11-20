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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Tag("plainAttrTable")
@Transactional("Master")
public class PlainAttrTest extends AbstractTest {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Test
    public void deleteAttr() {
        plainAttrDAO.delete(findPlainAttr("35f407a2-d254-4890-9e45-5a7dd8c8df7d", UPlainAttr.class));

        entityManager().flush();

        assertNull(findPlainAttr("35f407a2-d254-4890-9e45-5a7dd8c8df7d", UPlainAttr.class));
        assertNull(findPlainAttrValue("0c67225a-030a-4c56-b337-17cf7a311f0f", UPlainAttrValue.class));
    }

    @Test
    public void deleteAllAttValues() {
        UPlainAttrValue value = findPlainAttrValue("7034de3b-3687-4db5-8454-363468f1a9de", UPlainAttrValue.class);
        assertNotNull(value);

        plainAttrValueDAO.deleteAll(value.getAttr(), anyUtilsFactory.getInstance(AnyTypeKind.USER));

        value = findPlainAttrValue("7034de3b-3687-4db5-8454-363468f1a9de", UPlainAttrValue.class);
        assertNull(value);

        // by removing all values, the related attribute is not valid any more
        try {
            entityManager().flush();
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
    }
}
