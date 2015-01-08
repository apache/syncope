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
package org.apache.syncope.persistence.jpa.relationship;

import static org.junit.Assert.assertNull;

import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerSchemaTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Test
    public void test() {
        UDerSchema schema = derSchemaDAO.find("cn", UDerSchema.class);

        derSchemaDAO.delete(schema.getKey(), attrUtilFactory.getInstance(AttributableType.USER));

        derSchemaDAO.flush();

        assertNull(derSchemaDAO.find(schema.getKey(), UDerSchema.class));
        assertNull(derAttrDAO.find(100L, UDerAttr.class));
        assertNull(userDAO.find(3L).getDerAttr(schema.getKey()));
    }
}
