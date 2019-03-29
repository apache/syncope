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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.TestPullActions;
import org.junit.jupiter.api.Test;

public class ImplementationITCase extends AbstractITCase {

    @Test
    public void create() {
        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey(UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(IdMImplementationType.PUSH_ACTIONS);
        implementationTO.setBody(TestPullActions.class.getName());

        // fail because type is wrong
        try {
            implementationService.create(implementationTO);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidImplementation, e.getType());
        }
        implementationTO.setType(IdMImplementationType.PULL_ACTIONS);

        Response response = implementationService.create(implementationTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        ImplementationTO actual = implementationService.read(
                implementationTO.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(actual);
        assertEquals(actual, implementationTO);
    }

    @Test
    public void delete() {
        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey(UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(IdMImplementationType.PULL_ACTIONS);
        implementationTO.setBody(TestPullActions.class.getName());

        implementationService.create(implementationTO);

        PullTaskTO pullTask = taskService.read(TaskType.PULL, AbstractTaskITCase.PULL_TASK_KEY, false);
        assertNotNull(pullTask);

        int before = pullTask.getActions().size();

        pullTask.getActions().add(implementationTO.getKey());
        taskService.update(TaskType.PULL, pullTask);

        pullTask = taskService.read(TaskType.PULL, AbstractTaskITCase.PULL_TASK_KEY, false);
        assertNotNull(pullTask);

        int after = pullTask.getActions().size();
        assertEquals(before + 1, after);

        // fails because the implementation is used
        try {
            implementationService.delete(implementationTO.getType(), implementationTO.getKey());
            fail("Unexpected");
        } catch (SyncopeClientException e) {
            assertEquals(e.getType(), ClientExceptionType.InUse);
        }

        pullTask.getActions().remove(implementationTO.getKey());
        taskService.update(TaskType.PULL, pullTask);

        implementationService.delete(implementationTO.getType(), implementationTO.getKey());
    }

}
