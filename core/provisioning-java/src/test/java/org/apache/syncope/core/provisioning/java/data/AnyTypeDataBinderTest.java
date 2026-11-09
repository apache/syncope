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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnyTypeDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private AnyTypeDataBinder binder;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    void get() {
        AnyTypeTO user = binder.getAnyTypeTO(anyTypeDAO.findById(AnyTypeKind.USER.name()).orElseThrow());
        assertEquals(AnyTypeKind.USER.name(), user.getKey());
        assertEquals(AnyTypeKind.USER, user.getKind());
        assertTrue(user.getClasses().containsAll(List.of("minimal user", "other")));

        AnyTypeTO printer = binder.getAnyTypeTO(anyTypeDAO.findById("PRINTER").orElseThrow());
        assertEquals("PRINTER", printer.getKey());
        assertEquals(AnyTypeKind.ANY_OBJECT, printer.getKind());
        assertTrue(printer.getClasses().contains("minimal printer"));
    }

    @Test
    void createUpdateDelete() {
        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey("TEST_TYPE");
        anyTypeTO.setKind(AnyTypeKind.ANY_OBJECT);
        anyTypeTO.getClasses().add("minimal printer");
        anyTypeTO.getClasses().add("invalid-class");

        AnyType anyType = binder.create(anyTypeTO);
        anyType = anyTypeDAO.save(anyType);

        AnyTypeTO created = binder.getAnyTypeTO(anyType);
        assertEquals("TEST_TYPE", created.getKey());
        assertEquals(List.of("minimal printer"), created.getClasses());

        created.getClasses().clear();
        created.getClasses().add("csv");
        binder.update(anyType, created);
        assertEquals(List.of("csv"), binder.getAnyTypeTO(anyType).getClasses());

        AnyTypeTO deleted = binder.delete(anyType);
        assertEquals("TEST_TYPE", deleted.getKey());
        assertEquals(List.of("csv"), deleted.getClasses());
    }

    @Test
    void kindCannotChange() {
        AnyType user = anyTypeDAO.findById(AnyTypeKind.USER.name()).orElseThrow();

        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey(AnyTypeKind.USER.name());
        anyTypeTO.setKind(AnyTypeKind.GROUP);

        assertThrows(SyncopeClientException.class, () -> binder.update(user, anyTypeTO));
    }
}
