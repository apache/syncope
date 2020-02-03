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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class PropagationByResourceTest extends AbstractTest {

    private final String key = "testKey";

    private final PropagationByResource<String> propagationByResource = new PropagationByResource<>();

    @Test
    public void voidMethods(
            @Mock Set<String> toBeCreated,
            @Mock Set<String> toBeUpdated,
            @Mock Set<String> toBeDeleted,
            @Mock Map<String, String> oldConnObjectKeys,
            @Mock PropagationByResource<String> propByRes) {
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
        propagationByResource.addOldConnObjectKey(key, oldConnObjectKey);
        verify(oldConnObjectKeys).put(key, oldConnObjectKey);
        propagationByResource.addOldConnObjectKey(key, null);
        verify(oldConnObjectKeys, times(0)).put(key, null);
        propagationByResource.addOldConnObjectKey(null, null);
        verify(oldConnObjectKeys, times(0)).put(null, null);
    }

    @Test
    public void add() {
        assertTrue(propagationByResource.add(ResourceOperation.CREATE, key));
        assertTrue(propagationByResource.add(ResourceOperation.UPDATE, key));
        assertTrue(propagationByResource.add(ResourceOperation.DELETE, key));
        assertFalse(propagationByResource.add(ResourceOperation.NONE, key));
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
        assertFalse(propagationByResource.remove(ResourceOperation.CREATE, key));
        assertFalse(propagationByResource.remove(ResourceOperation.UPDATE, key));
        assertFalse(propagationByResource.remove(ResourceOperation.DELETE, key));
        assertFalse(propagationByResource.remove(ResourceOperation.NONE, key));
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
        assertFalse(propagationByResource.contains(ResourceOperation.CREATE, key));
        assertFalse(propagationByResource.contains(ResourceOperation.UPDATE, key));
        assertFalse(propagationByResource.contains(ResourceOperation.DELETE, key));
        assertFalse(propagationByResource.contains(ResourceOperation.NONE, key));

        Set<String> matchingList = new HashSet<>();
        matchingList.add(key);
        assertFalse(propagationByResource.contains(key));

        ReflectionTestUtils.setField(propagationByResource, "toBeDeleted", matchingList);
        assertTrue(propagationByResource.contains(key));
    }

    @Test
    public void get() {
        Set<String> matchingList = new HashSet<>();
        matchingList.add(key);

        ReflectionTestUtils.setField(propagationByResource, "toBeDeleted", matchingList);
        assertEquals(matchingList, propagationByResource.get(ResourceOperation.DELETE));
        assertEquals(Collections.<String>emptySet(), propagationByResource.get(ResourceOperation.CREATE));
        assertEquals(Collections.<String>emptySet(), propagationByResource.get(ResourceOperation.UPDATE));
        assertEquals(Collections.<String>emptySet(), propagationByResource.get(ResourceOperation.NONE));

    }

    @Test
    public void asMap() {
        assertEquals(Collections.emptyMap(), propagationByResource.asMap());
    }

    @Test
    public void set() {
        Set<String> keys = new HashSet<>();
        keys.add("testKey1");
        keys.add("testKey2");

        propagationByResource.set(ResourceOperation.CREATE, Collections.<String>emptySet());
        assertEquals(Collections.emptySet(), ReflectionTestUtils.getField(propagationByResource, "toBeCreated"));
        propagationByResource.set(ResourceOperation.CREATE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeCreated"));
        propagationByResource.set(ResourceOperation.UPDATE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeUpdated"));
        propagationByResource.set(ResourceOperation.DELETE, keys);
        assertEquals(keys, ReflectionTestUtils.getField(propagationByResource, "toBeDeleted"));
    }
}
