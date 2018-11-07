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

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.SAML2BindingType;

@XmlRootElement(name = "saml2request")
@XmlType
public class SAML2RequestTO implements Serializable {

    private static final long serialVersionUID = -2454209295007372086L;

    private String idpServiceAddress;

    private SAML2BindingType bindingType;

    private String content;

    private String relayState;

    private String signAlg;

    private String signature;

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }

    public void setIdpServiceAddress(final String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }

    public SAML2BindingType getBindingType() {
        return bindingType;
    }

    public void setBindingType(final SAML2BindingType bindingType) {
        this.bindingType = bindingType;
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

    public String getSignAlg() {
        return signAlg;
    }

    public void setSignAlg(final String signAlg) {
        this.signAlg = signAlg;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(final String signature) {
        this.signature = signature;
    }

}
