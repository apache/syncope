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
package org.apache.syncope.core.persistence.common;

import org.apache.syncope.common.lib.types.CipherAlgorithm;

public abstract class AbstractDomainProperties {

    private String key;

    private String adminPassword;

    private CipherAlgorithm adminCipherAlgorithm = CipherAlgorithm.SHA512;

    private String content;

    private String keymasterConfParams;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public CipherAlgorithm getAdminCipherAlgorithm() {
        return adminCipherAlgorithm;
    }

    public void setAdminCipherAlgorithm(final CipherAlgorithm adminCipherAlgorithm) {
        this.adminCipherAlgorithm = adminCipherAlgorithm;
    }

    public String getContent() {
        return content == null
                ? "classpath:domains/" + key + "Content.xml"
                : content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getKeymasterConfParams() {
        return keymasterConfParams == null
                ? "classpath:domains/" + key + "KeymasterConfParams.json"
                : keymasterConfParams;
    }

    public void setKeymasterConfParams(final String keymasterConfParams) {
        this.keymasterConfParams = keymasterConfParams;
    }
}
