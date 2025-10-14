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
package org.apache.syncope.common.lib.password;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.to.PasswordManagementTO;

public class RESTPasswordManagementConf implements PasswordManagementConf {

    private static final long serialVersionUID = 7262269548010007815L;

    /**
     * Password for Basic-Auth at the password management endpoints.
     */
    private String endpointPassword;

    /**
     * Endpoint URL to use when unlocking account.
     */
    private String endpointUrlAccountUnlock;

    /**
     * Endpoint URL to use when updating passwords.
     */
    private String endpointUrlChange;

    /**
     * Endpoint URL to use when locating email addresses.
     */
    private String endpointUrlEmail;

    /**
     * Endpoint URL to use when locating phone numbers.
     */
    private String endpointUrlPhone;

    /**
     * Endpoint URL to use when locating security questions.
     */
    private String endpointUrlSecurityQuestions;

    /**
     * Endpoint URL to use when locating user names.
     */
    private String endpointUrlUser;

    /**
     * Endpoint URL to use when locating usernames.
     */
    private String endpointUsername;

    /**
     * Field name for oldPassword field when password change requests are submitted.
     */
    private String fieldNamePasswordOld = "oldPassword";

    /**
     * Field name for password field when password change requests are submitted.
     */
    private String fieldNamePassword = "password";

    /**
     * Field name for username field when password change requests are submitted.
     */
    private String fieldNameUser = "username";

    /**
     * Additional headers to be included in REST API calls for password management.
     * The map keys are header names and the corresponding values are header values.
     */
    private final Map<String, String> headers = new HashMap<>();

    public String getEndpointPassword() {
        return endpointPassword;
    }

    public void setEndpointPassword(final String endpointPassword) {
        this.endpointPassword = endpointPassword;
    }

    public String getEndpointUrlAccountUnlock() {
        return endpointUrlAccountUnlock;
    }

    public void setEndpointUrlAccountUnlock(final String endpointUrlAccountUnlock) {
        this.endpointUrlAccountUnlock = endpointUrlAccountUnlock;
    }

    public String getEndpointUrlChange() {
        return endpointUrlChange;
    }

    public void setEndpointUrlChange(final String endpointUrlChange) {
        this.endpointUrlChange = endpointUrlChange;
    }

    public String getEndpointUrlEmail() {
        return endpointUrlEmail;
    }

    public void setEndpointUrlEmail(final String endpointUrlEmail) {
        this.endpointUrlEmail = endpointUrlEmail;
    }

    public String getEndpointUrlPhone() {
        return endpointUrlPhone;
    }

    public void setEndpointUrlPhone(final String endpointUrlPhone) {
        this.endpointUrlPhone = endpointUrlPhone;
    }

    public String getEndpointUrlSecurityQuestions() {
        return endpointUrlSecurityQuestions;
    }

    public void setEndpointUrlSecurityQuestions(final String endpointUrlSecurityQuestions) {
        this.endpointUrlSecurityQuestions = endpointUrlSecurityQuestions;
    }

    public String getEndpointUrlUser() {
        return endpointUrlUser;
    }

    public void setEndpointUrlUser(final String endpointUrlUser) {
        this.endpointUrlUser = endpointUrlUser;
    }

    public String getEndpointUsername() {
        return endpointUsername;
    }

    public void setEndpointUsername(final String endpointUsername) {
        this.endpointUsername = endpointUsername;
    }

    public String getFieldNamePasswordOld() {
        return fieldNamePasswordOld;
    }

    public void setFieldNamePasswordOld(final String fieldNamePasswordOld) {
        this.fieldNamePasswordOld = fieldNamePasswordOld;
    }

    public String getFieldNamePassword() {
        return fieldNamePassword;
    }

    public void setFieldNamePassword(final String fieldNamePassword) {
        this.fieldNamePassword = fieldNamePassword;
    }

    public String getFieldNameUser() {
        return fieldNameUser;
    }

    public void setFieldNameUser(final String fieldNameUser) {
        this.fieldNameUser = fieldNameUser;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, Object> map(final PasswordManagementTO passwordManagementTO, final Mapper mapper) {
        return mapper.map(passwordManagementTO, this);
    }
}
