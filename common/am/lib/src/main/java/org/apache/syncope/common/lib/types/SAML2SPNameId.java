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
package org.apache.syncope.common.lib.types;

public enum SAML2SPNameId {

    EMAIL_ADDRESS("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"),
    UNSPECIFIED("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"),
    ENTITY("urn:oasis:names:tc:SAML:2.0:nameid-format:entity"),
    PERSISTENT("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"),
    TRANSIENT("urn:oasis:names:tc:SAML:2.0:nameid-format:transient"),
    ENCRYPTED("urn:oasis:names:tc:SAML:2.0:nameid-format:encrypted");

    private final String nameId;

    SAML2SPNameId(final String nameId) {
        this.nameId = nameId;
    }

    public String getNameId() {
        return nameId;
    }
}
