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
package org.syncope.core.persistence.dao;

import java.util.HashSet;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.Task;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;

@Transactional
public class TaskTest extends AbstractTest {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void findAll() {
        List<Task> list = taskDAO.findAll();
        assertEquals(3, list.size());
    }

    @Test
    public final void save() {
        TargetResource resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);

        Task task = new Task();
        task.setResource(resource);
        task.setPropagationMode(PropagationMode.ASYNC);
        task.setResourceOperationType(ResourceOperationType.CREATE);
        task.setAccountId("one@two.com");

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build("testAttribute", "testValue1",
                "testValue2"));
        attributes.add(
                AttributeBuilder.buildPassword("password".toCharArray()));
        task.setAttributes(attributes);

        task = taskDAO.save(task);
        assertNotNull(task);

        Task actual = taskDAO.find(task.getId());
        assertEquals(task, actual);
    }

    @Test
    public final void delete() {
        Task task = taskDAO.find(1L);
        assertNotNull(task);

        TargetResource resource = task.getResource();
        assertNotNull(resource);

        taskDAO.delete(task);
        task = taskDAO.find(1L);
        assertNull(task);

        resource = resourceDAO.find(resource.getName());
        assertNotNull(resource);
        assertFalse(resource.getTasks().contains(task));
    }
}
