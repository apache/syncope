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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "saml2spMetadataKeystore")
@XmlType
public class SAML2SPKeystoreTO extends BaseBean implements EntityTO {

    private static final long serialVersionUID = 3211073386484148953L;

    private String key;

    private String keystore;

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

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
            append(key).
            append(keystore).
            append(owner).
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
        SAML2SPKeystoreTO other = (SAML2SPKeystoreTO) obj;
        return new EqualsBuilder().
            append(key, other.key).
            append(keystore, other.keystore).
            append(owner, other.owner).
            build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("key", key)
            .append("owner", owner)
            .toString();
    }

    public static class Builder {

        private final SAML2SPKeystoreTO instance = new SAML2SPKeystoreTO();

        public Builder keystore(final String keystore) {
            instance.setKeystore(keystore);
            return this;
        }

        public Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public SAML2SPKeystoreTO build() {
            return instance;
        }
    }

}
