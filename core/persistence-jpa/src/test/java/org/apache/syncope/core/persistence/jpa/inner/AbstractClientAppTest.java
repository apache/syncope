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
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractClientAppTest extends AbstractTest {

    @Autowired
    protected PolicyDAO policyDAO;

    protected AttrReleasePolicy buildAndSaveAttrRelPolicy() {
        AttrReleasePolicy attrRelPolicy = entityFactory.newEntity(AttrReleasePolicy.class);
        attrRelPolicy.setName("AttrRelPolicyTest");
        attrRelPolicy.setStatus(Boolean.TRUE);

        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getAllowedAttrs().addAll(List.of("cn", "givenName"));
        conf.getIncludeOnlyAttrs().add("cn");

        attrRelPolicy.setConf(conf);

        return policyDAO.save(attrRelPolicy);

    }

    protected AccessPolicy buildAndSaveAccessPolicy() {
        AccessPolicy accessPolicy = entityFactory.newEntity(AccessPolicy.class);
        accessPolicy.setName("AccessPolicyTest");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setEnabled(true);
        conf.setSsoEnabled(false);
        conf.getRequiredAttrs().put("attribute1", "value1,value2");
        accessPolicy.setConf(conf);

        return policyDAO.save(accessPolicy);
    }

    protected AuthPolicy buildAndSaveAuthPolicy() {
        AuthPolicy authPolicy = entityFactory.newEntity(AuthPolicy.class);
        authPolicy.setName("AuthPolicyTest");

        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.getAuthModules().addAll(List.of("LdapAuthentication1", "DatabaseAuthentication2"));
        authPolicy.setConf(conf);

        return policyDAO.save(authPolicy);
    }

    protected TicketExpirationPolicy buildAndSaveTicketExpirationPolicy() {
        TicketExpirationPolicy ticketExpirationPolicy = entityFactory.newEntity(TicketExpirationPolicy.class);
        ticketExpirationPolicy.setName("TicketExpirationPolicyTest");

        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(110);
        conf.setTgtConf(tgtConf);
        DefaultTicketExpirationPolicyConf.STConf stConf = new DefaultTicketExpirationPolicyConf.STConf();
        stConf.setMaxTimeToLiveInSeconds(0);
        stConf.setNumberOfUses(1);
        conf.setStConf(stConf);
        ticketExpirationPolicy.setConf(conf);

        return policyDAO.save(ticketExpirationPolicy);
    }
}
