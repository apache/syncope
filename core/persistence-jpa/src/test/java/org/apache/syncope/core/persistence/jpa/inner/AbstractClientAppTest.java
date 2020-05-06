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
package org.apache.syncope.core.persistence.jpa.inner;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.AllowedAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractClientAppTest extends AbstractTest {

    @Autowired
    protected PolicyDAO policyDAO;

    protected AttrReleasePolicy buildAndSaveAttrRelPolicy() {
        AttrReleasePolicy attrRelPolicy = entityFactory.newEntity(AttrReleasePolicy.class);
        attrRelPolicy.setDescription("AttrRelPolicyTest");

        AllowedAttrReleasePolicyConf conf = new AllowedAttrReleasePolicyConf();
        conf.setName("Example Attr Rel Policy for an application");
        conf.getAllowedAttrs().addAll(List.of("cn", "givenName"));
        attrRelPolicy.setConf(conf);

        return policyDAO.save(attrRelPolicy);

    }

    protected AccessPolicy buildAndSaveAccessPolicy() {
        AccessPolicy accessPolicy = entityFactory.newEntity(AccessPolicy.class);
        accessPolicy.setDescription("AccessPolicyTest");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setEnabled(true);
        conf.setName("Example Access Policy for an application");
        conf.getRequiredAttrs().putAll(Map.of("attribute1", Set.of("value1", "value2")));
        conf.setSsoEnabled(false);
        accessPolicy.setConf(conf);

        return policyDAO.save(accessPolicy);
    }

    protected AuthPolicy buildAndSaveAuthPolicy() {
        AuthPolicy authPolicy = entityFactory.newEntity(AuthPolicy.class);
        authPolicy.setDescription("AuthPolicyTest");

        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.getAuthModules().addAll(List.of("LdapAuthentication1", "DatabaseAuthentication2"));
        authPolicy.setConf(conf);

        return policyDAO.save(authPolicy);
    }
}
