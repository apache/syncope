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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import org.junit.jupiter.api.Test;

class UserRequestFormTest {

    @Test
    void datesAreCopied() {
        UserRequestForm form = new UserRequestForm();

        Date createTime = new Date(1_000L);
        form.setCreateTime(createTime);
        createTime.setTime(2_000L);

        Date dueDate = new Date(3_000L);
        form.setDueDate(dueDate);
        dueDate.setTime(4_000L);

        assertEquals(1_000L, form.getCreateTime().getTime());
        assertEquals(3_000L, form.getDueDate().getTime());

        Date returnedCreateTime = form.getCreateTime();
        assertNotSame(form.getCreateTime(), returnedCreateTime);
        returnedCreateTime.setTime(5_000L);
        assertEquals(1_000L, form.getCreateTime().getTime());

        form.setCreateTime(null);
        form.setDueDate(null);
        assertNull(form.getCreateTime());
        assertNull(form.getDueDate());
    }

    @Test
    void equality() {
        UserRequestForm first = form("process", "task");
        UserRequestForm second = form("process", "task");
        UserRequestForm different = form("process", "other");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
        assertNotEquals(first, null);
        assertNotEquals(first, "not a form");
    }

    private static UserRequestForm form(final String bpmnProcess, final String taskId) {
        UserRequestForm form = new UserRequestForm();
        form.setBpmnProcess(bpmnProcess);
        form.setTaskId(taskId);
        form.setUsername("username");
        form.setCreateTime(new Date(1_000L));
        return form;
    }
}
