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
package org.apache.syncope.common.keymaster.client.zookeeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { ZookeeperKeymasterClientContext.class, ZookeeperTestContext.class })
public class ZookeeperServiceOpsITCase {

    @Autowired
    private ServiceOps serviceOps;

    @Test
    public void run() {
        List<NetworkService> list = serviceOps.list(NetworkService.Type.CORE);
        assertTrue(list.isEmpty());

        NetworkService core1 = new NetworkService();
        core1.setType(NetworkService.Type.CORE);
        core1.setAddress("http://localhost:9080/syncope/rest");
        serviceOps.register(core1);

        list = serviceOps.list(NetworkService.Type.CORE);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(core1, list.getFirst());

        assertEquals(core1, serviceOps.get(NetworkService.Type.CORE));

        NetworkService core2 = new NetworkService();
        core2.setType(NetworkService.Type.CORE);
        core2.setAddress("http://localhost:9080/syncope/rest");
        assertEquals(core1, core2);
        serviceOps.register(core2);

        list = serviceOps.list(NetworkService.Type.CORE);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(core1, list.getFirst());

        assertEquals(core1, serviceOps.get(NetworkService.Type.CORE));

        serviceOps.unregister(core1);
        list = serviceOps.list(NetworkService.Type.CORE);
        assertTrue(list.isEmpty());

        try {
            serviceOps.get(NetworkService.Type.CORE);
            fail();
        } catch (KeymasterException e) {
            assertNotNull(e);
        }
    }
}
