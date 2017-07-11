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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnyObjectTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void find() {
        AnyObject anyObject = anyObjectDAO.find("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(anyObject);
        assertNotNull(anyObject.getType());
        assertFalse(anyObject.getType().getClasses().isEmpty());
    }

    @Test
    public void save() {
        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("a name");
        anyObject.setType(anyTypeDAO.find("PRINTER"));
        anyObject.setRealm(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM));

        anyObject = anyObjectDAO.save(anyObject);
        assertNotNull(anyObject);
    }

    @Test
    public void delete() {
        AnyObject anyObject = anyObjectDAO.find("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        anyObjectDAO.delete(anyObject.getKey());

        AnyObject actual = anyObjectDAO.find("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNull(actual);
    }
}
