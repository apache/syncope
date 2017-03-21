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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "saml2LoginResponse")
@XmlType
public class SAML2LoginResponseTO extends AbstractBaseBean {

    private static final long serialVersionUID = 794772343787258010L;

    private String nameID;

    private String sessionIndex;

    private Date authInstant;

    private Date notOnOrAfter;

    private String accessToken;

    private String username;

    private final Set<AttrTO> attrs = new HashSet<>();

    private String idp;

    private boolean sloSupported;

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

    public Date getAuthInstant() {
        if (authInstant != null) {
            return new Date(authInstant.getTime());
        }
        return null;
    }

    public void setAuthInstant(final Date authInstant) {
        if (authInstant != null) {
            this.authInstant = new Date(authInstant.getTime());
        } else {
            this.authInstant = null;
        }
    }

    public Date getNotOnOrAfter() {
        if (notOnOrAfter != null) {
            return new Date(notOnOrAfter.getTime());
        }
        return null;
    }

    public void setNotOnOrAfter(final Date notOnOrAfter) {
        if (notOnOrAfter != null) {
            this.notOnOrAfter = new Date(notOnOrAfter.getTime());
        } else {
            this.notOnOrAfter = null;
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @XmlElementWrapper(name = "attrs")
    @XmlElement(name = "attr")
    @JsonProperty("attrs")
    public Set<AttrTO> getAttrs() {
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

}
