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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.ClientAppTO;

public class WAClientApp implements BaseBean {

    private static final long serialVersionUID = 6633251825655119506L;

    private ClientAppTO clientAppTO;

    private AccessPolicyTO accessPolicy;

    private final List<AuthModuleTO> authModules = new ArrayList<>();

    private AuthPolicyTO authPolicy;

    private AttrReleasePolicyTO attrReleasePolicy;

    private TicketExpirationPolicyTO ticketExpirationPolicy;

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

    @JacksonXmlElementWrapper(localName = "authModules")
    @JacksonXmlProperty(localName = "authModule")
    public List<AuthModuleTO> getAuthModules() {
        return authModules;
    }

    public AuthPolicyTO getAuthPolicy() {
        return authPolicy;
    }

    public void setAuthPolicy(final AuthPolicyTO authPolicy) {
        this.authPolicy = authPolicy;
    }

    public AttrReleasePolicyTO getAttrReleasePolicy() {
        return attrReleasePolicy;
    }

    public void setAttrReleasePolicy(final AttrReleasePolicyTO attrReleasePolicy) {
        this.attrReleasePolicy = attrReleasePolicy;
    }

    public TicketExpirationPolicyTO getTicketExpirationPolicy() {
        return ticketExpirationPolicy;
    }

    public void setTicketExpirationPolicy(final TicketExpirationPolicyTO ticketExpirationPolicy) {
        this.ticketExpirationPolicy = ticketExpirationPolicy;
    }
}
