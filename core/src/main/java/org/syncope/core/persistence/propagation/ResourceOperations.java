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
import java.util.HashSet;
import java.util.Set;
import org.syncope.core.persistence.beans.TargetResource;

public class ResourceOperations {

    public enum Type {

        CREATE, UPDATE, DELETE
    }
    private Set<TargetResource> toBeCreated;
    private Set<TargetResource> toBeUpdated;
    private Set<TargetResource> toBeDeleted;

    public ResourceOperations() {
        toBeCreated = new HashSet<TargetResource>();
        toBeUpdated = new HashSet<TargetResource>();
        toBeDeleted = new HashSet<TargetResource>();
    }

    /**
     * Avoid potential conflicts by not doing create or update on any
     * resource for which a delete is requested, and by not doing any create
     * on any resource for which an update is requested.
     */
    public void purge() {
        for (TargetResource resource : toBeDeleted) {
            toBeCreated.remove(resource);
            toBeUpdated.remove(resource);
        }
        for (TargetResource resource : toBeUpdated) {
            toBeCreated.remove(resource);
        }
    }

    public boolean add(Type type, TargetResource resource) {
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
        }

        return result;
    }

    public boolean remove(Type type, TargetResource resource) {
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
        }

        return result;
    }

    public Set<TargetResource> get(Type type) {
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
        }

        return result;
    }

    public void set(Type type, Set<TargetResource> resources) {
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
        }
    }

    public void merge(ResourceOperations resourceOperations) {
        toBeCreated.addAll(resourceOperations.get(Type.CREATE));
        toBeUpdated.addAll(resourceOperations.get(Type.UPDATE));
        toBeDeleted.addAll(resourceOperations.get(Type.DELETE));
    }
}
