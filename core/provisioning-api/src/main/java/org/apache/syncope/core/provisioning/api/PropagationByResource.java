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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.types.ResourceOperation;

/**
 * Encapsulates operations to be performed on various resources.
 *
 * @param <T> key for propagation: could be simple resource or pair (resource, connObjectKeyValue) for linked accounts
 */
public class PropagationByResource<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -5699740428104336636L;

    /**
     * Resources for creation.
     */
    private final Set<T> toBeCreated;

    /**
     * Resources for update.
     */
    private final Set<T> toBeUpdated;

    /**
     * Resources for deletion.
     */
    private final Set<T> toBeDeleted;

    /**
     * Mapping target resource names to old ConnObjectKeys (when applicable).
     */
    private final Map<String, String> oldConnObjectKeys;

    /**
     * Default constructor.
     */
    public PropagationByResource() {
        toBeCreated = new HashSet<>();
        toBeUpdated = new HashSet<>();
        toBeDeleted = new HashSet<>();

        oldConnObjectKeys = new HashMap<>();
    }

    /**
     * Avoid potential conflicts by not doing create or update on any resource for which a delete is requested, and by
     * not doing any create on any resource for which an update is requested.
     */
    public final void purge() {
        toBeCreated.removeAll(toBeDeleted);
        toBeCreated.removeAll(toBeUpdated);

        toBeUpdated.removeAll(toBeDeleted);
    }

    /**
     * Add an element.
     *
     * @param type resource operation type
     * @param key target resource
     * @return whether the operation was successful or not
     */
    public final boolean add(final ResourceOperation type, final T key) {
        Set<T> set;
        switch (type) {
            case CREATE:
                set = toBeCreated;
                break;

            case UPDATE:
                set = toBeUpdated;
                break;

            case DELETE:
            default:
                set = toBeDeleted;
                break;
        }

        return set.add(key);
    }

    /**
     * Add some elements.
     *
     * @param type resource operation type
     * @param keys target resources
     * @return whether the operation was successful or not
     */
    public boolean addAll(final ResourceOperation type, final Collection<T> keys) {
        Set<T> set;
        switch (type) {
            case CREATE:
                set = toBeCreated;
                break;

            case UPDATE:
                set = toBeUpdated;
                break;

            case DELETE:
            default:
                set = toBeDeleted;
                break;
        }

        return set.addAll(keys);
    }

    /**
     * Remove an element.
     *
     * @param type resource operation type
     * @param key target resource
     * @return whether the operation was successful or not
     */
    public final boolean remove(final ResourceOperation type, final T key) {
        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.remove(key);
                break;

            case UPDATE:
                result = toBeUpdated.remove(key);
                break;

            case DELETE:
                result = toBeDeleted.remove(key);
                break;

            default:
        }

        return result;
    }

    /**
     * Remove some elements.
     *
     * @param type resource operation type
     * @param keys target resources
     * @return whether the operation was successful or not
     */
    public boolean removeAll(final ResourceOperation type, final Set<T> keys) {
        Set<T> set;
        switch (type) {
            case CREATE:
                set = toBeCreated;
                break;

            case UPDATE:
                set = toBeUpdated;
                break;

            case DELETE:
            default:
                set = toBeDeleted;
                break;
        }

        return set.removeAll(keys);
    }

    /**
     * Removes only the resource names in the underlying resource name sets that are contained in the specified
     * collection.
     *
     * @param keys collection containing resource names to be retained in the underlying resource name sets
     * @return {@code true} if the underlying resource name sets changed as a result of the call
     * @see Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(final Collection<T> keys) {
        return toBeCreated.removeAll(keys)
                || toBeUpdated.removeAll(keys)
                || toBeDeleted.removeAll(keys);
    }

    /**
     * Retains only the resource names in the underlying resource name sets that are contained in the specified
     * collection.
     *
     * @param keys collection containing resource names to be retained in the underlying resource name sets
     * @return {@code true} if the underlying resource name sets changed as a result of the call
     * @see Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(final Collection<T> keys) {
        return toBeCreated.retainAll(keys)
                || toBeUpdated.retainAll(keys)
                || toBeDeleted.retainAll(keys);
    }

    public boolean contains(final ResourceOperation type, final T key) {
        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.contains(key);
                break;

            case UPDATE:
                result = toBeUpdated.contains(key);
                break;

            case DELETE:
                result = toBeDeleted.contains(key);
                break;

            default:
        }

        return result;
    }

    public boolean contains(final T key) {
        return toBeCreated.contains(key)
                || toBeUpdated.contains(key)
                || toBeDeleted.contains(key);
    }

    /**
     * Get resources for a given resource operation type.
     *
     * @param type resource operation type
     * @return resource matching the given type
     */
    public final Set<T> get(final ResourceOperation type) {
        Set<T> result = Set.of();

        switch (type) {
            case CREATE:
                result = toBeCreated;
                break;

            case UPDATE:
                result = toBeUpdated;
                break;

            case DELETE:
                result = toBeDeleted;
                break;

            default:
        }

        return result;
    }

    public Map<T, ResourceOperation> asMap() {
        Map<T, ResourceOperation> result = new HashMap<>();
        Stream.of(ResourceOperation.values()).
                forEach(operation -> get(operation).forEach(resource -> result.put(resource, operation)));

        return result;
    }

    /**
     * Set resources for a given resource operation type.
     *
     * @param type resource operation type
     * @param keys to be set
     */
    public final void set(final ResourceOperation type, final Collection<T> keys) {

        switch (type) {
            case CREATE:
                toBeCreated.clear();
                toBeCreated.addAll(keys);
                break;

            case UPDATE:
                toBeUpdated.clear();
                toBeUpdated.addAll(keys);
                break;

            case DELETE:
                toBeDeleted.clear();
                toBeDeleted.addAll(keys);
                break;

            default:
        }
    }

    /**
     * Merge another resource operation instance into this instance.
     *
     * @param propByRes to be merged
     */
    public final void merge(final PropagationByResource<T> propByRes) {
        if (propByRes != null) {
            toBeCreated.addAll(propByRes.get(ResourceOperation.CREATE));
            toBeUpdated.addAll(propByRes.get(ResourceOperation.UPDATE));
            toBeDeleted.addAll(propByRes.get(ResourceOperation.DELETE));
            oldConnObjectKeys.putAll(propByRes.getOldConnObjectKeys());
        }
    }

    /**
     * Removes all of the operations.
     */
    public void clear() {
        toBeCreated.clear();
        toBeUpdated.clear();
        toBeDeleted.clear();
    }

    /**
     * Whether no operations are present.
     *
     * @return true if no operations (create / update / delete) and no old connObjectKeys are present
     */
    public final boolean isEmpty() {
        return toBeCreated.isEmpty() && toBeUpdated.isEmpty() && toBeDeleted.isEmpty() && oldConnObjectKeys.isEmpty();
    }

    /**
     * Fetch all old connObjectKeys.
     *
     * @return old connObjectKeys; can be empty
     */
    public Map<String, String> getOldConnObjectKeys() {
        return oldConnObjectKeys;
    }

    /**
     * Fetch old connObjectKey for given resource name.
     *
     * @param resourceKey resource name
     * @return old connObjectKey; can be null
     */
    public String getOldConnObjectKey(final String resourceKey) {
        return oldConnObjectKeys.get(resourceKey);
    }

    /**
     * Add old ConnObjectKey for a given resource name.
     *
     * @param resourceKey resourceKey resource name
     * @param oldConnObjectKey old ConnObjectKey
     */
    public void addOldConnObjectKey(final String resourceKey, final String oldConnObjectKey) {
        if (resourceKey != null && oldConnObjectKey != null) {
            oldConnObjectKeys.put(resourceKey, oldConnObjectKey);
        }
    }

    @Override
    public String toString() {
        return "To be Created: " + toBeCreated + ";\n"
                + "To be Updated: " + toBeUpdated + ";\n"
                + "To be Deleted: " + toBeDeleted + ";\n"
                + "Old connObjectKeys: " + oldConnObjectKeys;
    }
}
