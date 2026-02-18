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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.net.URI;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

public class SerializationTest {

    private static final JsonMapper MAPPER = new SyncopeJsonMapper();

    @Test
    public void accessPolicyConf() {
        AccessPolicyTO policy = new AccessPolicyTO();
        policy.setName("Test Access policy");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setOrder(11);
        conf.setEnabled(true);
        conf.setUnauthorizedRedirectUrl(URI.create("https://syncope.apache.org"));
        conf.getRequiredAttrs().put("cn", "admin,Admin,TheAdmin");
        conf.getRejectedAttrs().put("uid", "plain");
        policy.setConf(conf);

        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, policy);

        AccessPolicyTO actual = MAPPER.readValue(writer.toString(), AccessPolicyTO.class);
        assertEquals(policy, actual);
    }

    @Test
    public void attrReleasePolicyConf() {
        AttrReleasePolicyTO policy = new AttrReleasePolicyTO();
        policy.setName("Test Attribute Release Policy");

        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.setPrincipalIdAttr("principalIdAttr");
        conf.getAllowedAttrs().add("allowed1");
        conf.getAllowedAttrs().add("allowed2");
        conf.getPrincipalAttrRepoConf().getAttrRepos().add("attrRepo1");
        policy.setConf(conf);

        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, policy);

        assertTrue(writer.toString().contains("attrRepo1"));

        AttrReleasePolicyTO actual = MAPPER.readValue(writer.toString(), AttrReleasePolicyTO.class);
        assertEquals(policy, actual);

        assertTrue(((DefaultAttrReleasePolicyConf) actual.getConf()).
                getPrincipalAttrRepoConf().getAttrRepos().contains("attrRepo1"));
    }
}
