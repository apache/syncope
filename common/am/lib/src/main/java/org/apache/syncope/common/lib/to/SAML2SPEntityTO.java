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

public class SAML2SPEntityTO extends SAML2EntityTO {

    private static final long serialVersionUID = 6215073386484048953L;

    public static class Builder extends SAML2EntityTO.Builder<SAML2SPEntityTO, Builder> {

        @Override
        protected SAML2SPEntityTO newInstance() {
            return new SAML2SPEntityTO();
        }

        public Builder keystore(final String keystore) {
            getInstance().setKeystore(keystore);
            return this;
        }

        public Builder metadata(final String metadata) {
            getInstance().setMetadata(metadata);
            return this;
        }
    }

    private String keystore;

    private String metadata;

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(keystore).
                append(metadata).
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
        SAML2SPEntityTO other = (SAML2SPEntityTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(keystore, other.keystore).
                append(metadata, other.metadata).
                build();
    }
}
