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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOperation;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SCIMDataBinderTest {

    private SCIMDataBinder dataBinder;

    private static Stream<String> getValue() {
        return Stream.of("True", "False");
    }

    @BeforeAll
    void setup() {
        SCIMConfManager scimConfManager = mock(SCIMConfManager.class);
        when(scimConfManager.get()).thenReturn(new SCIMConf());
        UserLogic userLogic = mock(UserLogic.class);
        AuthDataAccessor authDataAccessor = mock(AuthDataAccessor.class);
        dataBinder = new SCIMDataBinder(scimConfManager, userLogic, authDataAccessor);
    }

    @ParameterizedTest
    @MethodSource("getValue")
    void toUserUpdate(final String value) {
        SCIMPatchOperation operation = new SCIMPatchOperation();
        SCIMPatchPath scimPatchPath = new SCIMPatchPath();
        scimPatchPath.setAttribute("active");
        operation.setPath(scimPatchPath);
        operation.setValue(List.of(value));
        assertDoesNotThrow(() -> dataBinder.toUserUpdate(new UserTO(), List.of(), operation));
    }
}
