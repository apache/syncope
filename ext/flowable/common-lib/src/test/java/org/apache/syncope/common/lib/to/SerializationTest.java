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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.form.FormProperty;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.common.lib.form.FormPropertyValue;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.junit.jupiter.api.Test;

public abstract class SerializationTest {

    protected abstract ObjectMapper objectMapper();

    @Test
    public void userRequestForm() throws IOException {
        UserRequestForm form = new UserRequestForm();
        form.setBpmnProcess("process");
        form.setCreateTime(new Date());
        form.setUsername("username");
        form.setExecutionId("434343");
        form.setFormKey("123456");

        UserTO userTO = new UserTO();
        userTO.setKey(UUID.randomUUID().toString());
        form.setUserTO(userTO);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("schema1").value("value1").build()).build());
        form.setUserUR(userUR);

        FormProperty property = new FormProperty();
        property.setId("printMode");
        property.setName("Preferred print mode");
        property.setType(FormPropertyType.Dropdown);
        property.getDropdownValues().add(
                new FormPropertyValue("8559d14d-58c2-46eb-a2d4-a7d35161e8f8", "value1"));
        property.getDropdownValues().add(
                new FormPropertyValue(UUID.randomUUID().toString(), "value2 / value3"));
        form.getProperties().add(property);

        PagedResult<UserRequestForm> original = new PagedResult<>();
        original.getResult().add(form);
        original.setSize(1);
        original.setTotalCount(1);

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, original);

        PagedResult<UserRequestForm> actual = objectMapper().readValue(writer.toString(), new TypeReference<>() {
        });
        assertEquals(original, actual);
    }
}
