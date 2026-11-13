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
package org.apache.syncope.core.flowable.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.common.engine.api.FlowableException;
import org.junit.jupiter.api.Test;

class FlowableRuntimeUtilsTest {

    @Test
    void encryptAndDecrypt() {
        String clear = "password to be encrypted";

        String encrypted = FlowableRuntimeUtils.encrypt(clear);

        assertEquals(clear, FlowableRuntimeUtils.decrypt(encrypted));
    }

    @Test
    void businessKeys() {
        assertEquals("process:user", FlowableRuntimeUtils.getProcBusinessKey("process", "user"));
        assertEquals(
                FlowableRuntimeUtils.WF_PROCESS_ID + ":user",
                FlowableRuntimeUtils.getWFProcBusinessKey("user"));

        Pair<String, String> split = FlowableRuntimeUtils.splitProcBusinessKey("process:user");
        assertEquals("process", split.getLeft());
        assertEquals("user", split.getRight());

        assertThrows(WorkflowException.class, () -> FlowableRuntimeUtils.splitProcBusinessKey("process"));
        assertThrows(WorkflowException.class, () -> FlowableRuntimeUtils.splitProcBusinessKey("process:user:extra"));
    }

    @Test
    void throwExceptionWithoutCause() {
        FlowableException flowableException = new FlowableException("failure");

        WorkflowException actual = assertThrows(
                WorkflowException.class,
                () -> FlowableRuntimeUtils.throwException(flowableException, "default message"));

        assertEquals("default message", actual.getMessage());
        assertSame(flowableException, actual.getCause());
    }

    @Test
    void throwExceptionWithSyncopeClientException() {
        SyncopeClientException cause = SyncopeClientException.build(ClientExceptionType.InvalidRequest);

        assertSame(
                cause,
                assertThrows(
                        SyncopeClientException.class,
                        () -> FlowableRuntimeUtils.throwException(new FlowableException("failure", cause), "default")));
    }

    @Test
    void throwExceptionWithValidationExceptions() {
        ParsingValidationException parsing = new ParsingValidationException("parsing", null);
        assertSame(
                parsing,
                assertThrows(
                        ParsingValidationException.class,
                        () -> FlowableRuntimeUtils.throwException(
                                new FlowableException("failure", parsing), "default")));

        InvalidEntityException invalid = new InvalidEntityException(
                FlowableRuntimeUtilsTest.class, EntityViolationType.Standard, "invalid");
        assertSame(
                invalid,
                assertThrows(
                        InvalidEntityException.class,
                        () -> FlowableRuntimeUtils.throwException(
                                new FlowableException("failure", invalid), "default")));
    }

    @Test
    void throwExceptionWithPersistenceCause() {
        RuntimeException persistence = new NotFoundException("not found");

        assertSame(
                persistence,
                assertThrows(
                        RuntimeException.class,
                        () -> FlowableRuntimeUtils.throwException(
                                new FlowableException("failure", persistence), "default")));
    }

    @Test
    void throwExceptionWithGenericCause() {
        IllegalStateException cause = new IllegalStateException("generic");

        FlowableException flowableException = new FlowableException("failure", cause);

        WorkflowException actual = assertThrows(
                WorkflowException.class,
                () -> FlowableRuntimeUtils.throwException(flowableException, "default"));

        assertInstanceOf(FlowableException.class, actual.getCause());
        assertSame(flowableException, actual.getCause());
    }
}
