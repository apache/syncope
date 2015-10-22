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
package org.apache.syncope.common.lib.types;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for encapsulating operations to be performed on various resources.
 */
public class PropagationByResource implements Serializable {

    private static final long serialVersionUID = -5699740428104336636L;

    /**
     * Resources for creation.
     */
    private final Set<String> toBeCreated;

    /**
     * Resources for update.
     */
    private final Set<String> toBeUpdated;

    /**
     * Resources for deletion.
     */
    private final Set<String> toBeDeleted;

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
     * @param resourceName target resource
     * @return whether the operation was successful or not
     */
    public final boolean add(final ResourceOperation type, final String resourceName) {
        Set<String> set;
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

        return set.add(resourceName);
    }

    /**
     * Add some elements.
     *
     * @param type resource operation type
     * @param resourceNames target resources
     * @return whether the operation was successful or not
     */
    public boolean addAll(final ResourceOperation type, final Collection<String> resourceNames) {
        Set<String> set;
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

        return set.addAll(resourceNames);
    }

    /**
     * Remove an element.
     *
     * @param type resource operation type
     * @param resourceName target resource
     * @return whether the operation was successful or not
     */
    public final boolean remove(final ResourceOperation type, final String resourceName) {
        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.remove(resourceName);
                break;

            case UPDATE:
                result = toBeUpdated.remove(resourceName);
                break;

            case DELETE:
                result = toBeDeleted.remove(resourceName);
                break;

            default:
        }

        return result;
    }

    /**
     * Remove some elements.
     *
     * @param type resource operation type
     * @param resourceNames target resources
     * @return whether the operation was successful or not
     */
    public boolean removeAll(final ResourceOperation type, final Set<String> resourceNames) {
        Set<String> set;
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

        return set.removeAll(resourceNames);
    }

    /**
     * Removes only the resource names in the underlying resource name sets that are contained in the specified
     * collection.
     *
     * @param resourceNames collection containing resource names to be retained in the underlying resource name sets
     * @return <tt>true</tt> if the underlying resource name sets changed as a result of the call
     * @see Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(final Collection<String> resourceNames) {
        return toBeCreated.removeAll(resourceNames)
                | toBeUpdated.removeAll(resourceNames)
                | toBeDeleted.removeAll(resourceNames);
    }

    /**
     * Retains only the resource names in the underlying resource name sets that are contained in the specified
     * collection.
     *
     * @param resourceNames collection containing resource names to be retained in the underlying resource name sets
     * @return <tt>true</tt> if the underlying resource name sets changed as a result of the call
     * @see Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(final Collection<String> resourceNames) {
        return toBeCreated.retainAll(resourceNames)
                | toBeUpdated.retainAll(resourceNames)
                | toBeDeleted.retainAll(resourceNames);
    }

    public boolean contains(final ResourceOperation type, final String resourceName) {
        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.contains(resourceName);
                break;

            case UPDATE:
                result = toBeUpdated.contains(resourceName);
                break;

            case DELETE:
                result = toBeDeleted.contains(resourceName);
                break;

            default:
        }

        return result;
    }

    /**
     * Get resources for a given resource operation type.
     *
     * @param type resource operation type
     * @return resource matching the given type
     */
    public final Set<String> get(final ResourceOperation type) {
        Set<String> result = Collections.<String>emptySet();

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

    public Map<String, ResourceOperation> asMap() {
        Map<String, ResourceOperation> result = new HashMap<>();
        for (ResourceOperation operation : ResourceOperation.values()) {
            for (String resourceName : get(operation)) {
                result.put(resourceName, operation);
            }
        }

        return result;
    }

    /**
     * Set resources for a given resource operation type.
     *
     * @param type resource operation type
     * @param resourceNames to be set
     */
    public final void set(final ResourceOperation type, final Collection<String> resourceNames) {

        switch (type) {
            case CREATE:
                toBeCreated.clear();
                toBeCreated.addAll(resourceNames);
                break;

            case UPDATE:
                toBeUpdated.clear();
                toBeUpdated.addAll(resourceNames);
                break;

            case DELETE:
                toBeDeleted.clear();
                toBeDeleted.addAll(resourceNames);
                break;

            default:
        }
    }

    /**
     * Merge another resource operation instance into this instance.
     *
     * @param propByRes to be merged
     */
    public final void merge(final PropagationByResource propByRes) {
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
     * @param resourceName resource name
     * @return old connObjectKey; can be null
     */
    public String getOldConnObjectKey(final String resourceName) {
        return oldConnObjectKeys.get(resourceName);
    }

    /**
     * Add old ConnObjectKey for a given resource name.
     *
     * @param resourceName resourceName resource name
     * @param oldConnObjectKey old ConnObjectKey
     */
    public void addOldConnObjectKey(final String resourceName, final String oldConnObjectKey) {
        if (resourceName != null && oldConnObjectKey != null) {
            oldConnObjectKeys.put(resourceName, oldConnObjectKey);
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
