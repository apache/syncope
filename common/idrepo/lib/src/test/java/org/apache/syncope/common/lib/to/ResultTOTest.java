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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.junit.jupiter.api.Test;

class ResultTOTest {

    @Test
    void pagedResult() {
        PagedResult<UserTO> result = new PagedResult<>();
        result.setPrev(URI.create("/prev"));
        result.setNext(URI.create("/next"));
        result.setPage(1);
        result.setSize(10);
        result.setTotalCount(100);
        UserTO user = new UserTO();
        user.setKey("key");
        result.getResult().add(user);

        assertEquals(URI.create("/prev"), result.getPrev());
        assertEquals(URI.create("/next"), result.getNext());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(100, result.getTotalCount());
        assertEquals(1, result.getResult().size());
    }

    @Test
    void pagedResultEqualsAndHashCode() {
        PagedResult<UserTO> first = new PagedResult<>();
        first.setPage(1);
        first.setSize(10);
        first.setTotalCount(100);
        UserTO user = new UserTO();
        user.setKey("key");
        first.getResult().add(user);

        PagedResult<UserTO> second = new PagedResult<>();
        second.setPage(1);
        second.setSize(10);
        second.setTotalCount(100);
        UserTO sameUser = new UserTO();
        sameUser.setKey("key");
        second.getResult().add(sameUser);

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        second.setPage(2);
        assertNotEquals(first, second);
    }

    @Test
    void provisioningResult() {
        UserTO user = new UserTO();
        user.setKey("key");
        PropagationStatus status = new PropagationStatus();
        status.setResource("resource");
        status.setStatus(ExecStatus.SUCCESS);
        status.setFailureReason("failure");

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(user);
        result.getPropagationStatuses().add(status);

        assertEquals(user, result.getEntity());
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals("resource", status.getResource());
        assertEquals(ExecStatus.SUCCESS, status.getStatus());
        assertEquals("failure", status.getFailureReason());
    }

    @Test
    void provisioningResultEqualsAndHashCode() {
        UserTO user = new UserTO();
        user.setKey("key");
        UserTO sameUser = new UserTO();
        sameUser.setKey("key");
        UserTO otherUser = new UserTO();
        otherUser.setKey("other");

        ProvisioningResult<UserTO> first = new ProvisioningResult<>();
        first.setEntity(user);
        ProvisioningResult<UserTO> second = new ProvisioningResult<>();
        second.setEntity(sameUser);
        ProvisioningResult<UserTO> other = new ProvisioningResult<>();
        other.setEntity(otherUser);

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void propagationStatusEqualsAndHashCode() {
        PropagationStatus first = new PropagationStatus();
        first.setResource("resource");
        PropagationStatus second = new PropagationStatus();
        second.setResource("resource");
        PropagationStatus other = new PropagationStatus();
        other.setResource("other");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
        assertTrue(first.getFailureReason() == null);
    }
}
