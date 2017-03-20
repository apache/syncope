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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "saml2request")
@XmlType
public class SAML2RequestTO extends AbstractBaseBean {

    private static final long serialVersionUID = -2454209295007372086L;

    private String idpServiceAddress;

    private String content;

    private String relayState;

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }

    public void setIdpServiceAddress(final String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(final String relayState) {
        this.relayState = relayState;
    }

}
