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
package org.apache.syncope.core.logic.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

public class ClassPathScanImplementationLookupTest {

    private ClassPathScanImplementationLookup lookup;

    @BeforeEach
    void setUp() {
        ImplementationTypesHolder.getInstance().putAll(IdRepoImplementationType.values());
        ImplementationTypesHolder.getInstance().putAll(IdMImplementationType.values());

        lookup = new ClassPathScanImplementationLookup();
        lookup.load();
    }

    @Test
    void order() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, lookup.getOrder());
    }

    @Test
    void classNames() {
        IdRepoImplementationType.values().keySet().
                forEach(type -> assertNotNull(lookup.getClassNames(type)));
    }

    @Test
    void unknownConfigurationClasses() {
        assertNull(lookup.getReportClass(null));
        assertNull(lookup.getAccountRuleClass(null));
        assertNull(lookup.getPasswordRuleClass(null));
        assertNull(lookup.getInboundCorrelationRuleClass(null));
        assertNull(lookup.getPushCorrelationRuleClass(null));
    }
}
