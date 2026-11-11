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
package org.apache.syncope.common.lib.scim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SCIMEnterpriseUserConfTest {

    @Test
    void asMapOnlyMappedAttributes() {
        SCIMManagerConf manager = new SCIMManagerConf();
        manager.setKey("manager");

        SCIMEnterpriseUserConf conf = new SCIMEnterpriseUserConf();
        conf.setEmployeeNumber("employeeNumber");
        conf.setCostCenter("costCenter");
        conf.setOrganization("organization");
        conf.setDivision("division");
        conf.setDepartment("department");
        conf.setManager(manager);

        assertEquals(Map.of(
                "employeeNumber", "employeeNumber",
                "costCenter", "costCenter",
                "organization", "organization",
                "division", "division",
                "department", "department"),
                conf.asMap());
        assertFalse(conf.asMap().containsKey("manager"));
        assertThrows(UnsupportedOperationException.class, () -> conf.asMap().put("other", "other"));
    }

    @Test
    void asMapSkipsNullAttributes() {
        SCIMEnterpriseUserConf conf = new SCIMEnterpriseUserConf();
        conf.setOrganization("organization");

        assertEquals(Map.of("organization", "organization"), conf.asMap());
        assertTrue(conf.asMap().containsKey("organization"));
    }
}
