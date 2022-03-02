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
package org.apache.syncope.common.lib.saml2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;

public class SAML2LoginResponse implements Serializable {

    private static final long serialVersionUID = 794772343787258010L;

    private String nameID;

    private String sessionIndex;

    private Date notOnOrAfter;

    private String accessToken;

    private OffsetDateTime accessTokenExpiryTime;

    private String username;

    private final Set<Attr> attrs = new HashSet<>();

    private String idp;

    private boolean sloSupported;

    private boolean selfReg;

    public String getNameID() {
        return nameID;
    }

    public void setNameID(final String nameID) {
        this.nameID = nameID;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public void setSessionIndex(final String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public Date getNotOnOrAfter() {
        return Optional.ofNullable(notOnOrAfter).map(date -> new Date(date.getTime())).orElse(null);
    }

    public void setNotOnOrAfter(final Date notOnOrAfter) {
        this.notOnOrAfter = Optional.ofNullable(notOnOrAfter).
                map(date -> new Date(date.getTime())).orElse(null);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public OffsetDateTime getAccessTokenExpiryTime() {
        return accessTokenExpiryTime;
    }

    public void setAccessTokenExpiryTime(final OffsetDateTime accessTokenExpiryTime) {
        this.accessTokenExpiryTime = accessTokenExpiryTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @JsonIgnore
    public Optional<Attr> getAttr(final String schema) {
        return attrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "attrs")
    @JacksonXmlProperty(localName = "attr")
    public Set<Attr> getAttrs() {
        return attrs;
    }

    public String getIdp() {
        return idp;
    }

    public void setIdp(final String idp) {
        this.idp = idp;
    }

    public boolean isSloSupported() {
        return sloSupported;
    }

    public void setSloSupported(final boolean sloSupported) {
        this.sloSupported = sloSupported;
    }

    public boolean isSelfReg() {
        return selfReg;
    }

    public void setSelfReg(final boolean selfReg) {
        this.selfReg = selfReg;
    }
}
