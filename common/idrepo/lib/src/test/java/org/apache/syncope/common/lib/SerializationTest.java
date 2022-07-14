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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.junit.jupiter.api.Test;

public abstract class SerializationTest {

    protected abstract ObjectMapper objectMapper();

    @Test
    public void emptyListAsRoot() throws IOException {
        List<ReportTO> original = new ArrayList<>();

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, original);

        List<ReportTO> actual = objectMapper().readValue(writer.toString(),
            new TypeReference<>() {
            });
        assertEquals(original, actual);
    }

    @Test
    public void nonEmptyListAsMember() throws IOException {
        AnyObjectCR original = new AnyObjectCR();
        original.setName("newPrinter");
        original.setType("PRINTER");
        original.getPlainAttrs().add(new Attr.Builder("location").value("new").build());

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, original);

        AnyObjectCR actual = objectMapper().readValue(writer.toString(), AnyObjectCR.class);
        assertEquals(original, actual);
    }

    @Test
    public void map() throws IOException {
        GroupUR req = new GroupUR();
        req.setKey(UUID.randomUUID().toString());
        req.getADynMembershipConds().put("key1", "value1");
        req.getADynMembershipConds().put("key2", "value2");

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, req);

        GroupUR actual = objectMapper().readValue(writer.toString(), GroupUR.class);
        assertEquals(req, actual);
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

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, req);

        UserUR actual = objectMapper().readValue(writer.toString(), UserUR.class);
        assertEquals(req, actual);
    }

    @Test
    public void pagedResult() throws IOException {
        GroupTO group = new GroupTO();
        group.setName(UUID.randomUUID().toString());
        group.setRealm(SyncopeConstants.ROOT_REALM);
        group.getPlainAttrs().add(new Attr.Builder("style").value("cool").value("great").build());
        group.getADynMembershipConds().put("USER", "username==a*");

        PagedResult<GroupTO> original = new PagedResult<>();
        original.getResult().add(group);
        original.setSize(1);
        original.setTotalCount(1);

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, original);

        PagedResult<GroupTO> actual = objectMapper().readValue(writer.toString(),
            new TypeReference<>() {
            });
        assertEquals(original, actual);
    }

    @Test
    public void provisioningResult() throws IOException {
        GroupTO group = new GroupTO();
        group.setName(UUID.randomUUID().toString());
        group.setRealm(SyncopeConstants.ROOT_REALM);
        group.getVirAttrs().add(new Attr.Builder("rvirtualdata").value("rvirtualvalue").build());
        group.getADynMembershipConds().put("USER", "username==a*");

        ProvisioningResult<GroupTO> original = new ProvisioningResult<>();
        original.setEntity(group);

        PropagationStatus status = new PropagationStatus();
        status.setFailureReason("failed");
        status.setBeforeObj(new ConnObject());
        original.getPropagationStatuses().add(status);

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, original);

        ProvisioningResult<GroupTO> actual = objectMapper().readValue(writer.toString(),
            new TypeReference<>() {
            });
        assertEquals(original, actual);
    }
}
