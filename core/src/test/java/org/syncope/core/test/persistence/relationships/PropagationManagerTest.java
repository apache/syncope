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
package org.syncope.core.test.persistence.relationships;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.persistence.propagation.ResourceOperations.Type;
import org.syncope.core.test.persistence.AbstractTest;

@Transactional
public class PropagationManagerTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;
    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private PropagationManager propagationManager;

    @Test
    public final void create() {
        SyncopeUser user = syncopeUserDAO.find(1L);
        assertNotNull(user);

        TargetResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        user.setResources(Collections.singleton(resource));

        Set<String> provisioned = null;
        try {
            provisioned = propagationManager.create(user);
        } catch (PropagationException e) {
            LOG.error("While provisioning", e);
        }

        assertNotNull(provisioned);
    }

    @Test
    public final void update() {
        SyncopeUser user = syncopeUserDAO.find(2L);
        assertNotNull(user);

        TargetResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        user.setResources(Collections.singleton(resource));

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.add(Type.UPDATE, resource);

        Set<String> provisioned = null;
        try {
            provisioned = propagationManager.update(
                    user, resourceOperations, null);
        } catch (PropagationException e) {
            LOG.error("While updating", e);
        }

        assertNotNull(provisioned);
    }

    @Test
    public final void createWithException() {
        SyncopeUser user = syncopeUserDAO.find(3L);
        assertNotNull(user);

        TargetResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        user.setResources(Collections.singleton(resource));

        PropagationException re = null;
        try {
            propagationManager.create(
                    user, Collections.singleton("ws-target-resource-2"));
        } catch (PropagationException e) {
            re = e;
        }
        assertNotNull(re);
    }

    @Test
    public final void updateWithException() {
        SyncopeUser user = syncopeUserDAO.find(4L);
        assertNotNull(user);

        TargetResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        user.setResources(Collections.singleton(resource));

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.add(Type.UPDATE, resource);

        PropagationException re = null;
        try {
            propagationManager.update(user, resourceOperations,
                    Collections.singleton("ws-target-resource-2"));
        } catch (PropagationException e) {
            re = e;
        }
        assertNotNull(re);
    }
}
