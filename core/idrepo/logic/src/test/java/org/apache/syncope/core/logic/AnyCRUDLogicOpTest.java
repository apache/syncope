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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class AnyCRUDLogicOpTest {

    @Test
    void contract() throws Exception {
        assertTrue(AnyCRUDLogicOp.class.isInterface());

        assertEquals(AnyTO.class,
                AnyCRUDLogicOp.class.getMethod("read", String.class).getReturnType());
        assertEquals(Page.class, AnyCRUDLogicOp.class.getMethod(
                "search", SearchCond.class, Pageable.class, String.class, boolean.class, boolean.class).
                getReturnType());
        assertEquals(ProvisioningResult.class, AnyCRUDLogicOp.class.getMethod(
                "create", AnyCR.class, boolean.class).getReturnType());
        assertEquals(ProvisioningResult.class, AnyCRUDLogicOp.class.getMethod(
                "update", AnyUR.class, boolean.class).getReturnType());
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "delete", String.class, boolean.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "unlink", String.class, Collection.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "link", String.class, Collection.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "unassign", String.class, Collection.class, boolean.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "assign", String.class, Collection.class, boolean.class, String.class, boolean.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "deprovision", String.class, List.class, boolean.class));
        assertNotNull(AnyCRUDLogicOp.class.getMethod(
                "provision", String.class, List.class, boolean.class, String.class, boolean.class));
    }
}
