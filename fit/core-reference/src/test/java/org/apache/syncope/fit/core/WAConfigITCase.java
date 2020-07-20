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
package org.apache.syncope.fit.core;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.WAConfigTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WAConfigITCase extends AbstractITCase {
    @Test
    public void verify() {
        WAConfigTO configTO = new WAConfigTO.Builder()
            .name(UUID.randomUUID().toString())
            .value("v1, v2, v3")
            .build();
        Response response = waConfigService.create(configTO);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);

        assertFalse(waConfigService.list().isEmpty());

        configTO = waConfigService.read(key);
        assertNotNull(configTO);

        configTO = waConfigService.readByName(configTO.getName());
        assertNotNull(configTO);

        configTO.setValue(1984);
        waConfigService.update(configTO);

        configTO = waConfigService.readByName(configTO.getName());
        assertEquals(1984, configTO.getValue());

        waConfigService.delete(key);
        assertThrows(SyncopeClientException.class, () -> waConfigService.read(key));
    }
}
