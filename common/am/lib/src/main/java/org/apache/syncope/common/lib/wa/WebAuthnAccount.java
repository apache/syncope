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
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class WebAuthnAccount implements BaseBean {

    private static final long serialVersionUID = 2285073386484048953L;

    public static class Builder {

        private final WebAuthnAccount instance = new WebAuthnAccount();

        public WebAuthnAccount.Builder credential(final WebAuthnDeviceCredential credential) {
            instance.getCredentials().add(credential);
            return this;
        }

        public WebAuthnAccount.Builder credentials(final WebAuthnDeviceCredential... credentials) {
            instance.getCredentials().addAll(List.of(credentials));
            return this;
        }

        public WebAuthnAccount.Builder credentials(final Collection<WebAuthnDeviceCredential> credentials) {
            instance.getCredentials().addAll(credentials);
            return this;
        }

        public WebAuthnAccount build() {
            return instance;
        }
    }

    private final List<WebAuthnDeviceCredential> credentials = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "credentials")
    @JacksonXmlProperty(localName = "credential")
    public List<WebAuthnDeviceCredential> getCredentials() {
        return credentials;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(credentials)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        WebAuthnAccount rhs = (WebAuthnAccount) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.credentials, rhs.credentials)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("credentials", credentials)
                .toString();
    }
}
