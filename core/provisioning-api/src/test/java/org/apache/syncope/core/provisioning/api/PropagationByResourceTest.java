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
package org.apache.syncope.core.provisioning.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class PropagationByResourceTest extends AbstractTest {

    private static final String KEY = "testKey";

    private final PropagationByResource<String> propagationByResource = new PropagationByResource<>();

    @Test
    public void voidMethods(
            final @Mock Set<String> toBeCreated,
            final @Mock Set<String> toBeUpdated,
            final @Mock Set<String> toBeDeleted,
            final @Mock Map<String, String> oldConnObjectKeys,
            final @Mock PropagationByResource<String> propByRes) {

        ReflectionTestUtils.setField(propagationByResource, "toBeCreated", toBeCreated);
        ReflectionTestUtils.setField(propagationByResource, "toBeUpdated", toBeUpdated);
        ReflectionTestUtils.setField(propagationByResource, "toBeDeleted", toBeDeleted);
        ReflectionTestUtils.setField(propagationByResource, "oldConnObjectKeys", oldConnObjectKeys);

        propagationByResource.purge();
        verify(toBeCreated).removeAll(toBeDeleted);
        verify(toBeCreated).removeAll(toBeUpdated);
        verify(toBeUpdated).removeAll(toBeDeleted);

        propagationByResource.clear();
        verify(toBeCreated).clear();
        verify(toBeCreated).clear();
        verify(toBeUpdated).clear();

        propagationByResource.merge(null);
        verify(toBeCreated, times(0)).addAll(any());
        verify(toBeUpdated, times(0)).addAll(any());
        verify(toBeDeleted, times(0)).addAll(any());
        verify(oldConnObjectKeys, times(0)).putAll(any());

        propagationByResource.merge(propByRes);
        verify(toBeCreated).addAll(any());
        verify(toBeUpdated).addAll(any());
        verify(toBeDeleted).addAll(any());

        String oldConnObjectKey = "oldConnObjectKey";
        propagationByResource.addOldConnObjectKey(KEY, oldConnObjectKey);
        verify(oldConnObjectKeys).put(KEY, oldConnObjectKey);
        propagationByResource.addOldConnObjectKey(KEY, null);
        verify(oldConnObjectKeys, times(0)).put(KEY, null);
        propagationByResource.addOldConnObjectKey(null, null);
        verify(oldConnObjectKeys, times(0)).put(null, null);
    }

    @Test
    public void add() {
        assertTrue(propagationByResource.add(ResourceOperation.CREATE, KEY));
        assertTrue(propagationByResource.add(ResourceOperation.UPDATE, KEY));
        assertTrue(propagationByResource.add(ResourceOperation.DELETE, KEY));
        assertFalse(propagationByResource.add(ResourceOperation.NONE, KEY));
    }

    @Test
    public void addAll() {
        List<String> keys = new ArrayList<>();
        keys.add("testKey1");
        keys.add("testKey2");

        assertTrue(propagationByResource.addAll(ResourceOperation.CREATE, keys));
        assertTrue(propagationByResource.addAll(ResourceOperation.UPDATE, keys));
        assertTrue(propagationByResource.addAll(ResourceOperation.DELETE, keys));
        assertFalse(propagationByResource.addAll(ResourceOperation.NONE, keys));
    }

    @Test
    public void remove() {
        assertFalse(propagationByResource.remove(ResourceOperation.CREATE, KEY));
        assertFalse(propagationByResource.remove(ResourceOperation.UPDATE, KEY));
        assertFalse(propagationByResource.remove(ResourceOperation.DELETE, KEY));
        assertFalse(propagationByResource.remove(ResourceOperation.NONE, KEY));
    }

    @Test
    public void removeAll() {
        Set<String> keys = new HashSet<>();
        keys.add("testKey1");
        keys.add("testKey2");

        assertFalse(propagationByResource.removeAll(ResourceOperation.CREATE, keys));
        assertFalse(propagationByResource.removeAll(ResourceOperation.UPDATE, keys));
        assertFalse(propagationByResource.removeAll(ResourceOperation.DELETE, keys));
        assertFalse(propagationByResource.removeAll(ResourceOperation.NONE, keys));
    }

    @Test
    public void removeAndRetainAll() {
        List<String> keys = new ArrayList<>();
        keys.add("testKey1");
        keys.add("testKey2");

        assertFalse(propagationByResource.removeAll(keys));
        assertFalse(propagationByResource.retainAll(keys));
    }

    @Test
    public void contains() {
        assertFalse(propagationByResource.contains(ResourceOperation.CREATE, KEY));
        assertFalse(propagationByResource.contains(ResourceOperation.UPDATE, KEY));
        assertFalse(propagationByResource.contains(ResourceOperation.DELETE, KEY));
        assertFalse(propagationByResource.contains(ResourceOperation.NONE, KEY));

        Set<String> matchingList = new HashSet<>();
        matchingList.add(KEY);
        assertFalse(propagationByResource.contains(KEY));

        ReflectionTestUtils.setField(propagationByResource, "toBeDeleted", matchingList);
        assertTrue(propagationByResource.contains(KEY));
    }

    @Test
    public void get() {
        Set<String> matchingList = new HashSet<>();
        matchingList.add(KEY);

        ReflectionTestUtils.setField(propagationByResource, "toBeDeleted", matchingList);
        assertEquals(matchingList, propagationByResource.get(ResourceOperation.DELETE));
        assertEquals(Set.of(), propagationByResource.get(ResourceOperation.CREATE));
        assertEquals(Set.of(), propagationByResource.get(ResourceOperation.UPDATE));
        assertEquals(Set.of(), propagationByResource.get(ResourceOperation.NONE));

    }

    @Test
    public void asMap() {
        assertEquals(Map.of(), propagationByResource.asMap());
    }

    @Test
    public void set() {
        Set<String> keys = new HashSet<>();
        keys.add("testKey1");
        keys.add("testKey2");

        propagationByResource.set(ResourceOperation.CREATE, Set.of());
        assertEquals(Set.of(), ReflectionTestUtils.getField(propagationByResource, "toBeCreated"));
        propagationByResource.set(ResourceOperation.CREATE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeCreated"));
        propagationByResource.set(ResourceOperation.UPDATE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeUpdated"));
        propagationByResource.set(ResourceOperation.DELETE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeDeleted"));
    }

    @Test
    public void byLinkedAccount() {
        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        propByLinkedAccount.add(ResourceOperation.CREATE, Pair.of("resource1", "connObjectKey1"));
        propByLinkedAccount.add(ResourceOperation.CREATE, Pair.of("resource2", "connObjectKey2"));

        assertEquals(2, propByLinkedAccount.asMap().size());
        assertEquals(ResourceOperation.CREATE, propByLinkedAccount.asMap().get(Pair.of("resource1", "connObjectKey1")));
        assertEquals(ResourceOperation.CREATE, propByLinkedAccount.asMap().get(Pair.of("resource2", "connObjectKey2")));

        Set<String> noPropResourceKeys = Set.of("resource2");
        propByLinkedAccount.get(ResourceOperation.CREATE).
                removeIf(account -> noPropResourceKeys.contains(account.getLeft()));

        assertEquals(1, propByLinkedAccount.asMap().size());
        assertEquals(ResourceOperation.CREATE, propByLinkedAccount.asMap().get(Pair.of("resource1", "connObjectKey1")));
    }
}
