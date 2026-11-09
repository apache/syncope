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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

public class AbstractTransactionalLogicTest {

    private static class TestLogic extends AbstractTransactionalLogic<AnyTypeClassTO> {

        @Override
        protected AnyTypeClassTO resolveReference(final Method method, final Object... args) {
            return new AnyTypeClassTO();
        }
    }

    @Test
    void transactionalAnnotated() {
        Transactional transactional = TestLogic.class.getAnnotation(Transactional.class);

        assertTrue(transactional != null);
        assertArrayEquals(new Class<?>[] { Throwable.class }, transactional.rollbackFor());
    }
}
