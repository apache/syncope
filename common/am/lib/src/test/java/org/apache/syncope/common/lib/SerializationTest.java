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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.junit.jupiter.api.Test;

public abstract class SerializationTest {

    protected abstract ObjectMapper objectMapper();

    @Test
    public void accessPolicyConf() throws IOException {
        AccessPolicyTO policy = new AccessPolicyTO();
        policy.setName("Test Access policy");
        policy.setEnabled(true);

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.getRequiredAttrs().add(new Attr.Builder("cn").values("admin", "Admin", "TheAdmin").build());
        policy.setConf(conf);

        StringWriter writer = new StringWriter();
        objectMapper().writeValue(writer, policy);

        AccessPolicyTO actual = objectMapper().readValue(writer.toString(), AccessPolicyTO.class);
        assertEquals(policy, actual);
    }
}
