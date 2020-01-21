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
package org.apache.syncope.core.provisioning.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class AuditEntryImplTest extends AbstractTest {

    @Mock
    private AuditLoggerName logger;

    private final String who = "testUser";

    private final Object before = "before";

    private final String output = "output";

    private final String[] input = { "test1", "test2" };
    
    private final String throwable = "throwable";
    
    private final String key = UUID.randomUUID().toString();
    
    private final Date date = new Date();

    @Test
    public void AuditEntryImpl() {
        AuditEntryImpl auditEntryImpl = new AuditEntryImpl(who, logger, before, output, input);
        assertEquals(who, auditEntryImpl.getWho());
        assertEquals(logger, auditEntryImpl.getLogger());
        assertEquals(output, auditEntryImpl.getOutput());
        assertEquals(input.length, auditEntryImpl.getInput().length);
        assertEquals(before, auditEntryImpl.getBefore());
        
        AuditEntryImpl auditEntryImpl2 = AuditEntryImpl.builder().
                input(null).
                date(date).
                key(key).
                throwable(throwable).
                build();
        assertEquals(date, auditEntryImpl2.getDate());
        assertEquals(throwable, auditEntryImpl2.getThrowable());
    }
}
