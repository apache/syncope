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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AbstractLogicTest {

    private static class TestLogic extends AbstractLogic<AnyTypeClassTO> {

        private AnyTypeClassTO resolved;

        @Override
        protected AnyTypeClassTO resolveReference(final Method method, final Object... args) {
            return resolved;
        }

        @Transactional(readOnly = true)
        public AnyTypeClassTO readOnly() {
            return null;
        }

        public AnyTypeClassTO readWrite() {
            return null;
        }
    }

    @Test
    void readOnlyIsAlwaysUnresolved() throws Exception {
        TestLogic logic = new TestLogic();

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(
                TestLogic.class.getMethod("readOnly")));
    }

    @Test
    void delegatesToResolveReference() throws Exception {
        TestLogic logic = new TestLogic();
        logic.resolved = new AnyTypeClassTO();
        logic.resolved.setKey("key");

        assertSame(logic.resolved, logic.resolveBeanReference(
                TestLogic.class.getMethod("readWrite")));
    }
}
