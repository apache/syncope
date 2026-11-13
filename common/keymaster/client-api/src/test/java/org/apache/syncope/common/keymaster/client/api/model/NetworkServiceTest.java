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
package org.apache.syncope.common.keymaster.client.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetworkServiceTest {

    @Test
    void networkService() {
        NetworkService service = new NetworkService();
        service.setType(NetworkService.Type.CORE);
        service.setAddress("http://localhost:8080");

        assertEquals(NetworkService.Type.CORE, service.getType());
        assertEquals("http://localhost:8080", service.getAddress());

        NetworkService same = new NetworkService();
        same.setType(NetworkService.Type.CORE);
        same.setAddress("http://localhost:8080");

        NetworkService other = new NetworkService();
        other.setType(NetworkService.Type.ENDUSER);

        assertEquals(service, service);
        assertEquals(service, same);
        assertEquals(service.hashCode(), same.hashCode());
        assertTrue(!service.equals(other));
    }
}
