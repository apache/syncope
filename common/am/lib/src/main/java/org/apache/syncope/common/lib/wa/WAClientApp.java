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
package org.apache.syncope.common.lib.wa;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AuthPolicyConf;
import org.apache.syncope.common.lib.to.ClientAppTO;

public class WAClientApp implements BaseBean {

    private static final long serialVersionUID = 6633251825655119506L;

    private ClientAppTO clientAppTO;

    private AccessPolicyTO accessPolicy;

    private AuthPolicyConf authPolicyConf;

    private AttrReleasePolicyConf attrReleasePolicyConf;

    private final Map<String, Object> releaseAttrs = new HashMap<>();

    public ClientAppTO getClientAppTO() {
        return clientAppTO;
    }

    public void setClientAppTO(final ClientAppTO clientAppTO) {
        this.clientAppTO = clientAppTO;
    }

    public AccessPolicyTO getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(final AccessPolicyTO accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public AuthPolicyConf getAuthPolicyConf() {
        return authPolicyConf;
    }

    public void setAuthPolicyConf(final AuthPolicyConf authPolicyConf) {
        this.authPolicyConf = authPolicyConf;
    }

    public AttrReleasePolicyConf getAttrReleasePolicyConf() {
        return attrReleasePolicyConf;
    }

    public void setAttrReleasePolicyConf(final AttrReleasePolicyConf attrReleasePolicyConf) {
        this.attrReleasePolicyConf = attrReleasePolicyConf;
    }

    public Map<String, Object> getReleaseAttrs() {
        return releaseAttrs;
    }
}
