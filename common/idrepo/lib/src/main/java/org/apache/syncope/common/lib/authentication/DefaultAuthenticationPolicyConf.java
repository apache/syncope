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
package org.apache.syncope.common.lib.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "defaultAuthenticationPolicyConf")
@XmlType
public class DefaultAuthenticationPolicyConf extends AbstractAuthenticationPolicyConf {

    private static final long serialVersionUID = 6021204813821798285L;

    /**
     * Authentication attribute.
     */
    private final List<String> authenticationAttributes = new ArrayList<>();

    /**
     * Case sensitive.
     */
    private boolean caseSensitiveAuthentication;

    public boolean isCaseSensitiveAuthentication() {
        return caseSensitiveAuthentication;
    }

    public void setCaseSensitiveAuthentication(final boolean caseSensitiveAuthentication) {
        this.caseSensitiveAuthentication = caseSensitiveAuthentication;
    }

    @XmlElementWrapper(name = "authenticationAttributes")
    @XmlElement(name = "attribute")
    @JsonProperty("authenticationAttributes")
    public List<String> getAuthenticationAttributes() {
        return authenticationAttributes;
    }
}
