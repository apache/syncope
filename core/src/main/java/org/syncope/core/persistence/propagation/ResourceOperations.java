/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.propagation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.types.ResourceOperationType;

/**
 * Utility class for encapsulating operations to be performed on various
 * resources.
 */
public class ResourceOperations {

    /**
     * Resources for creation.
     */
    private Set<TargetResource> toBeCreated;

    /**
     * Resources for update.
     */
    private Set<TargetResource> toBeUpdated;

    /**
     * Resources for deletion.
     */
    private Set<TargetResource> toBeDeleted;

    /**
     * Mapping target resource names to old account ids (when applicable).
     */
    private Map<String, String> oldAccountIds;

    /**
     * Default constructor.
     */
    public ResourceOperations() {
        toBeCreated = new HashSet<TargetResource>();
        toBeUpdated = new HashSet<TargetResource>();
        toBeDeleted = new HashSet<TargetResource>();

        oldAccountIds = new HashMap<String, String>();
    }

    /**
     * Avoid potential conflicts by not doing create or update on any
     * resource for which a delete is requested, and by not doing any create
     * on any resource for which an update is requested.
     */
    public final void purge() {
        for (TargetResource resource : toBeDeleted) {
            toBeCreated.remove(resource);
            toBeUpdated.remove(resource);
        }
        for (TargetResource resource : toBeUpdated) {
            toBeCreated.remove(resource);
        }
    }

    /**
     * Add an element.
     *
     * @param type resource operation type
     * @param resource target resource
     * @return wether the operation was succeful or not
     */
    public final boolean add(final ResourceOperationType type,
            final TargetResource resource) {

        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.add(resource);
                break;

            case UPDATE:
                result = toBeUpdated.add(resource);
                break;

            case DELETE:
                result = toBeDeleted.add(resource);
                break;

            default:
        }

        return result;
    }

    /**
     * Add some elements.
     *
     * @param type resource operation type
     * @param resources target resources
     * @return wether the operation was succeful or not
     */
    public boolean addAll(final ResourceOperationType type,
            final Set<TargetResource> resources) {

        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.addAll(resources);
                break;

            case UPDATE:
                result = toBeUpdated.addAll(resources);
                break;

            case DELETE:
                result = toBeDeleted.addAll(resources);
                break;

            default:
        }

        return result;
    }

    /**
     * Remove an element.
     *
     * @param type resource operation type
     * @param resource target resource
     * @return wether the operation was succeful or not
     */
    public final boolean remove(final ResourceOperationType type,
            final TargetResource resource) {

        boolean result = false;

        switch (type) {
            case CREATE:
                result = toBeCreated.remove(resource);
                break;

            case UPDATE:
                result = toBeUpdated.remove(resource);
                break;

            case DELETE:
                result = toBeDeleted.remove(resource);
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
    public final Set<TargetResource> get(final ResourceOperationType type) {
        Set<TargetResource> result = Collections.EMPTY_SET;

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

    /**
     * Set resources for a given resource operation type.
     *
     * @param type resource operation type
     * @param resources to be set
     */
    public final void set(final ResourceOperationType type,
            final Set<TargetResource> resources) {

        switch (type) {

            case CREATE:
                toBeCreated.clear();
                toBeCreated.addAll(resources);
                break;

            case UPDATE:
                toBeUpdated.clear();
                toBeUpdated.addAll(resources);
                break;

            case DELETE:
                toBeDeleted.clear();
                toBeDeleted.addAll(resources);
                break;

            default:
        }
    }

    /**
     * Merge another resource operation instance into this instance.
     *
     * @param resourceOperations to be merged
     */
    public final void merge(final ResourceOperations resourceOperations) {
        toBeCreated.addAll(
                resourceOperations.get(ResourceOperationType.CREATE));
        toBeUpdated.addAll(
                resourceOperations.get(ResourceOperationType.UPDATE));
        toBeDeleted.addAll(
                resourceOperations.get(ResourceOperationType.DELETE));
    }

    /**
     * Wether no operations are present.
     *
     * @return true if no operations (create / update / delete) are present
     */
    public final boolean isEmpty() {
        return toBeCreated.isEmpty()
                && toBeUpdated.isEmpty()
                && toBeUpdated.isEmpty();
    }

    /**
     * Fetch old account id for given resource name.
     *
     * @param resourceName resource name
     * @return old account id; can be null
     */
    public String getOldAccountId(final String resourceName) {
        return oldAccountIds.get(resourceName);
    }

    /**
     * Add old account id for a given resource name.
     *
     * @param resourceName resourceName resource name
     * @param oldAccountId old account id
     */
    public void addOldAccountId(final String resourceName,
            final String oldAccountId) {

        if (resourceName != null && oldAccountId != null) {
            oldAccountIds.put(resourceName, oldAccountId);
        }
    }

    @Override
    public String toString() {
        return "To be Created: " + toBeCreated + ";\n"
                + "To be Updated: " + toBeUpdated + ";\n"
                + "To be Deleted: " + toBeDeleted + ";\n"
                + "Old account Ids: " + oldAccountIds;
    }
}
