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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.UUID;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

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
        AuditEntryImpl auditEntryImpl2 = AuditEntryImpl.builder().
                who(who).
                before(before).
                logger(logger).
                output(output).
                input(null).
                date(date).
                key(key).
                throwable(throwable).
                build();

        assertEquals(auditEntryImpl2.getWho(), auditEntryImpl.getWho());
        assertEquals(auditEntryImpl2.getLogger(), auditEntryImpl.getLogger());
        assertNotEquals(auditEntryImpl2.getInput(), auditEntryImpl.getInput().length);
        assertEquals(auditEntryImpl2.getDate(), auditEntryImpl2.getDate());
        assertEquals(auditEntryImpl2.getThrowable(), auditEntryImpl2.getThrowable());
    }

    @Test
    public void AuditEntryImplWithUserTO(@Mock UserTO userTO) {
        AuditEntryImpl auditEntryImpl = new AuditEntryImpl(who, logger, before, userTO, input);
        assertTrue(EqualsBuilder.reflectionEquals(SerializationUtils.clone(userTO), auditEntryImpl.getOutput()));

        ReflectionTestUtils.setField(userTO, "password", "testP4ssw0rd!");
        ReflectionTestUtils.setField(userTO, "securityAnswer", "42");
        AuditEntryImpl auditEntryImpl2 = new AuditEntryImpl(who, logger, before, userTO, input);
        assertFalse(EqualsBuilder.reflectionEquals(SerializationUtils.clone(userTO), auditEntryImpl2.getOutput()));
    }

    @Test
    public void AuditEntryImplWithUserPatch(@Mock UserUR userUR) {
        AuditEntryImpl auditEntryImpl = new AuditEntryImpl(who, logger, userUR, output, input);
        assertTrue(EqualsBuilder.reflectionEquals(SerializationUtils.clone(userUR), auditEntryImpl.getBefore()));
    }
}
