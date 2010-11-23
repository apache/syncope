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
package org.syncope.core.rest;

import java.util.Arrays;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.TaskExecutionTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.types.TaskExecutionStatus;

public class TaskTestITCase extends AbstractTest {

    @Test
    public final void list() {
        List<TaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/list", TaskTO[].class));
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        for (TaskTO task : tasks) {
            assertNotNull(task);
        }
    }

    @Test
    public final void listExecutions() {
        List<TaskExecutionTO> executions = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/execution/list", TaskExecutionTO[].class));
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (TaskExecutionTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public final void read() {
        TaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", TaskTO.class, 1);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertFalse(taskTO.getExecutions().isEmpty());
    }

    @Test
    public final void readExecution() {
        TaskExecutionTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/execution/read/{taskId}",
                TaskExecutionTO.class, 1);
        assertNotNull(taskTO);
    }

    @Test
    public final void deal() {
        try {
            restTemplate.delete(BASE_URL + "task/delete/{taskId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        TaskExecutionTO execution = restTemplate.getForObject(
                BASE_URL + "task/execute/{taskId}",
                TaskExecutionTO.class, 1);
        assertEquals(TaskExecutionStatus.CREATED, execution.getStatus());

        Exception exception = null;
        try {
            restTemplate.delete(BASE_URL + "task/delete/{taskId}", 1);
        } catch (SyncopeClientCompositeErrorException scce) {
            assertTrue(scce.hasException(
                    SyncopeClientExceptionType.IncompleteTaskExecution));
            exception = scce;
        }
        assertNotNull(exception);

        execution = restTemplate.getForObject(
                BASE_URL + "task/execution/report/{executionId}"
                + "?executionStatus=SUCCESS&message=OK",
                TaskExecutionTO.class, execution.getId());
        assertEquals(TaskExecutionStatus.SUCCESS, execution.getStatus());
        assertEquals("OK", execution.getMessage());

        restTemplate.delete(BASE_URL + "task/delete/{taskId}", 1);
        try {
            restTemplate.getForObject(
                    BASE_URL + "task/execution/read/{executionId}",
                    TaskExecutionTO.class, execution.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}
