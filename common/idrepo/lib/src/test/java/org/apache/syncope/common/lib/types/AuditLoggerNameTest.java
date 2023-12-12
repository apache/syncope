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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditLoggerNameTest {

    @Test
    void custom() {
        AuditLoggerName aln = new AuditLoggerName(
                AuditElements.EventCategoryType.CUSTOM, null, null, "MY_EVENT", AuditElements.Result.SUCCESS);
        assertEquals("syncope.audit.[CUSTOM]:[]:[]:[MY_EVENT]:[SUCCESS]", aln.toAuditKey());
        assertEquals("[CUSTOM]:[]:[]:[MY_EVENT]:[SUCCESS]", aln.toString());

        assertEquals(aln, AuditLoggerName.fromAuditKey(aln.toAuditKey()));
    }
}
