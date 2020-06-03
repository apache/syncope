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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;

public class AuthProfileTO implements EntityTO {

    private static final long serialVersionUID = -6543425997956703057L;

    private final List<GoogleMfaAuthToken> googleMfaAuthTokens = new ArrayList<>();

    private String key;

    private String owner;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @JacksonXmlElementWrapper(localName = "googleMfaAuthTokens")
    @JacksonXmlProperty(localName = "googleMfaAuthToken")
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return googleMfaAuthTokens;
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().
            append(key).
            append(owner).
            append(googleMfaAuthTokens).
            build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthProfileTO other = (AuthProfileTO) obj;
        return new EqualsBuilder().
            append(key, other.key).
            append(owner, other.owner).
            append(googleMfaAuthTokens, other.googleMfaAuthTokens).
            build();
    }

    public static class Builder {

        private final AuthProfileTO instance = new AuthProfileTO();

        public AuthProfileTO.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public AuthProfileTO.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public AuthProfileTO build() {
            return instance;
        }
    }
}
