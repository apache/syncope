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
import static org.junit.Assert.assertNull;

import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainAttrTest extends AbstractTest {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Test
    public void deleteAttribute() {
        plainAttrDAO.delete("35f407a2-d254-4890-9e45-5a7dd8c8df7d", UPlainAttr.class);

        plainAttrDAO.flush();

        assertNull(plainAttrDAO.find("35f407a2-d254-4890-9e45-5a7dd8c8df7d", UPlainAttr.class));
        assertNull(
                plainAttrValueDAO.find("0c67225a-030a-4c56-b337-17cf7a311f0f", UPlainAttrValue.class));
    }

    @Test
    public void deleteAttributeValue() {
        UPlainAttrValue value = plainAttrValueDAO.find(
                "7034de3b-3687-4db5-8454-363468f1a9de", UPlainAttrValue.class);
        int attributeValueNumber = value.getAttr().getValues().size();

        plainAttrValueDAO.delete(value.getKey(), UPlainAttrValue.class);

        plainAttrValueDAO.flush();

        assertNull(plainAttrValueDAO.find(value.getKey(), UPlainAttrValue.class));

        UPlainAttr attribute = plainAttrDAO.find(
                "9d0d9e40-1b18-488e-9482-37dab82163c9", UPlainAttr.class);
        assertEquals(attribute.getValues().size(), attributeValueNumber - 1);
    }
}
