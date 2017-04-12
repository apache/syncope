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
package org.apache.syncope.client.enduser.model;

import java.io.Serializable;
import java.util.Map;

public class PlatformInfoRequest implements Serializable {

    private static final long serialVersionUID = -6763020920564016374L;

    private String version;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private boolean captchaEnabled;

    private Map<String, CustomAttributesInfo> customForm;

    public PlatformInfoRequest() {
    }

    public String getVersion() {
        return version;
    }

    public boolean isSelfRegAllowed() {
        return selfRegAllowed;
    }

    public boolean isPwdResetAllowed() {
        return pwdResetAllowed;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        return pwdResetRequiringSecurityQuestions;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setSelfRegAllowed(final boolean selfRegAllowed) {
        this.selfRegAllowed = selfRegAllowed;
    }

    public void setPwdResetAllowed(final boolean pwdResetAllowed) {
        this.pwdResetAllowed = pwdResetAllowed;
    }

    public void setPwdResetRequiringSecurityQuestions(final boolean pwdResetRequiringSecurityQuestions) {
        this.pwdResetRequiringSecurityQuestions = pwdResetRequiringSecurityQuestions;
    }

    public void setCaptchaEnabled(final boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public Map<String, CustomAttributesInfo> getCustomForm() {
        return customForm;
    }

    public void setCustomForm(final Map<String, CustomAttributesInfo> customForm) {
        this.customForm = customForm;
    }

}
