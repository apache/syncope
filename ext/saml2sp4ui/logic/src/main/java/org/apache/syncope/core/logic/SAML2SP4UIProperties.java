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
package org.apache.syncope.core.logic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("saml2.sp4ui")
public class SAML2SP4UIProperties {

    private String keystore;

    private String keystoreType;

    private String keystoreStorepass;

    private String keystoreKeypass;

    private String keystoreAlias;

    private long maximumAuthenticationLifetime = 3600;

    private long acceptedSkew = 300;

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(final String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getKeystoreStorepass() {
        return keystoreStorepass;
    }

    public void setKeystoreStorepass(final String keystoreStorepass) {
        this.keystoreStorepass = keystoreStorepass;
    }

    public String getKeystoreKeypass() {
        return keystoreKeypass;
    }

    public void setKeystoreKeypass(final String keystoreKeypass) {
        this.keystoreKeypass = keystoreKeypass;
    }

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(final String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    public long getMaximumAuthenticationLifetime() {
        return maximumAuthenticationLifetime;
    }

    public void setMaximumAuthenticationLifetime(final long maximumAuthenticationLifetime) {
        this.maximumAuthenticationLifetime = maximumAuthenticationLifetime;
    }

    public long getAcceptedSkew() {
        return acceptedSkew;
    }

    public void setAcceptedSkew(final long acceptedSkew) {
        this.acceptedSkew = acceptedSkew;
    }
}
