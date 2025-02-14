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

public enum OIDCTokenEncryptionAlg {
    none("none"),
    RSA1("RSA1_5"),
    RSA_OAEP("RSA-OAEP"),
    RSA_OAEP_256("RSA-OAEP-256"),
    A128KW("A128KW"),
    A192KW("A192KW"),
    A256KW("A256KW"),
    A128GCMKW("A128GCMKW"),
    A192GCMKW("A192GCMKW"),
    A256GCMKW("A256GCMKW"),
    ECDH_ES("ECDH-ES"),
    ECDH_ES_A128KW("ECDH-ES+A128KW"),
    ECDH_ES_A192KW("ECDH-ES+A192KW"),
    ECDH_ES_A256KW("ECDH-ES+A256KW");

    private final String externalForm;

    OIDCTokenEncryptionAlg(final String external) {
        this.externalForm = external;
    }

    public String getExternalForm() {
        return externalForm;
    }

}
