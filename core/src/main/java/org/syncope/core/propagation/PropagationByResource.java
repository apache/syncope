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
package org.syncope.core.propagation;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.syncope.types.PropagationOperation;

/**
 * Utility class for encapsulating operations to be performed on various
 * resources.
 */
public class PropagationByResource implements Serializable {

    private static final long serialVersionUID = -5699740428104336636L;

    /**
     * Resources for creation.
     */
    private Set<String> toBeCreated;

    /**
     * Resources for update.
     */
    private Set<String> toBeUpdated;

    /**
     * Resources for deletion.
     */
    private Set<String> toBeDeleted;

    /**
     * Mapping target resource names to old account ids (when applicable).
     */
    private Map<String, String> oldAccountIds;

    /**
     * Default constructor.
     */
    public PropagationByResource() {
        toBeCreated = new HashSet<String>();
        toBeUpdated = new HashSet<String>();
        toBeDeleted = new HashSet<String>();

        oldAccountIds = new HashMap<String, String>();
    }

    /**
     * Avoid potential conflicts by not doing create or update on any
     * resource for which a delete is requested, and by not doing any create
     * on any resource for which an update is requested.
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
     * @return wether the operation was succeful or not
     */
    public final boolean add(final PropagationOperation type,
            final String resourceName) {

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
     * @return wether the operation was succeful or not
     */
    public boolean addAll(final PropagationOperation type,
            final Set<String> resourceNames) {

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
     * @return wether the operation was succeful or not
     */
    public final boolean remove(final PropagationOperation type,
            final String resourceName) {

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
     * Get resources for a given resource operation type.
     *
     * @param type resource operation type
     * @return resource matching the given type
     */
    public final Set<String> get(final PropagationOperation type) {
        Set<String> result = Collections.EMPTY_SET;

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
     * @param resourceNames to be set
     */
    public final void set(final PropagationOperation type,
            final Set<String> resourceNames) {

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
        toBeCreated.addAll(
                propByRes.get(PropagationOperation.CREATE));
        toBeUpdated.addAll(
                propByRes.get(PropagationOperation.UPDATE));
        toBeDeleted.addAll(
                propByRes.get(PropagationOperation.DELETE));
        oldAccountIds.putAll(propByRes.getOldAccountIds());
    }

    /**
     * Wether no operations are present.
     *
     * @return true if no operations (create / update / delete) and no
     * old account ids are present
     */
    public final boolean isEmpty() {
        return toBeCreated.isEmpty()
                && toBeUpdated.isEmpty()
                && toBeDeleted.isEmpty()
                && oldAccountIds.isEmpty();
    }

    /**
     * Fetch all old account ids.
     *
     * @return old account ids; can be empty
     */
    public Map<String, String> getOldAccountIds() {
        return oldAccountIds;
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
