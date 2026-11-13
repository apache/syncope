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
package org.apache.syncope.client.console.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IdMConstantsTest {

    @Test
    void constants() {
        assertEquals("connid.locations.paginator.rows", IdMConstants.PREF_CONNID_LOCATIONS_PAGINATOR_ROWS);
        assertEquals("reconciliation.paginator.rows", IdMConstants.PREF_RECONCILIATION_PAGINATOR_ROWS);
        assertEquals("resource.status.paginator.rows", IdMConstants.PREF_RESOURCE_STATUS_PAGINATOR_ROWS);
        assertEquals("resources.paginator.rows", IdMConstants.PREF_RESOURCES_PAGINATOR_ROWS);
        assertEquals("remediation.paginator.rows", IdMConstants.PREF_REMEDIATION_PAGINATOR_ROWS);
        assertEquals("org.identityconnectors.common.security.GuardedString", IdMConstants.GUARDED_STRING);
        assertEquals("org.identityconnectors.common.security.GuardedByteArray", IdMConstants.GUARDED_BYTE_ARRAY);
    }
}
