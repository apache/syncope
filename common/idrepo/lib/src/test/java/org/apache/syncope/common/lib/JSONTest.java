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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.junit.jupiter.api.Test;

public class JSONTest {

    @Test
    public void map() throws IOException {
        GroupUR prop = new GroupUR();
        prop.getADynMembershipConds().put("key1", "value1");
        prop.getADynMembershipConds().put("key2", "value2");

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, prop);

        GroupUR unserializedProp = mapper.readValue(writer.toString(), GroupUR.class);
        assertEquals(prop, unserializedProp);
    }

    @Test
    public void patch() throws IOException {
        UserUR req = new UserUR();
        req.setKey(UUID.randomUUID().toString());
        req.setUsername(new StringReplacePatchItem.Builder().value("newusername").build());
        assertNotNull(req.getUsername().getValue());
        req.setPassword(new PasswordPatch.Builder().
                onSyncope(false).
                resource("ext1").resource("ext2").
                value("newpassword").
                build());
        assertNotNull(req.getPassword().getValue());
        req.getRoles().add(new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("role").build());

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, req);

        UserUR actual = mapper.readValue(writer.toString(), UserUR.class);
        assertEquals(req, actual);
    }
}
