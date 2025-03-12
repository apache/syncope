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
package org.apache.syncope.fit.core.wa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class WAConfigITCase extends AbstractITCase {

    private static Attr runTest(final List<String> initialValue, final List<String> updatedValue) {
        Attr config = new Attr.Builder(UUID.randomUUID().toString()).values(initialValue).build();
        WA_CONFIG_SERVICE.set(config);

        assertFalse(WA_CONFIG_SERVICE.list().isEmpty());

        config = WA_CONFIG_SERVICE.get(config.getSchema());
        assertNotNull(config);

        config = new Attr.Builder(config.getSchema()).values(updatedValue).build();
        WA_CONFIG_SERVICE.set(config);

        return WA_CONFIG_SERVICE.get(config.getSchema());
    }

    private static <T extends Serializable> void deleteEntry(final Attr configTO) {
        WA_CONFIG_SERVICE.delete(configTO.getSchema());
        assertThrows(SyncopeClientException.class, () -> WA_CONFIG_SERVICE.get(configTO.getSchema()));
    }

    @Test
    public void verify() {
        deleteEntry(runTest(List.of("v1", "v2"), List.of("newValue")));
        deleteEntry(runTest(List.of("12345"), List.of("98765")));
        deleteEntry(runTest(List.of("123.45"), List.of("987.65")));
        deleteEntry(runTest(List.of("1", "2", "3"), List.of("4", "5", "6")));
    }
}
