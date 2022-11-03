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
package org.apache.syncope.client.enduser.layout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SidebarLayout implements Serializable {

    private static final long serialVersionUID = 192032086034617378L;

    private boolean editUserEnabled = true;

    private boolean passwordManagementEnabled = true;

    private boolean securityQuestionManagementEnabled = true;

    private final Map<String, Boolean> extensionsEnabled = new HashMap<>();

    public boolean isEditUserEnabled() {
        return editUserEnabled;
    }

    public void setEditUserEnabled(final boolean editUserEnabled) {
        this.editUserEnabled = editUserEnabled;
    }

    public boolean isPasswordManagementEnabled() {
        return passwordManagementEnabled;
    }

    public void setPasswordManagementEnabled(final boolean passwordManagementEnabled) {
        this.passwordManagementEnabled = passwordManagementEnabled;
    }

    public boolean isSecurityQuestionManagementEnabled() {
        return securityQuestionManagementEnabled;
    }

    public void setSecurityQuestionManagementEnabled(final boolean securityQuestionManagementEnabled) {
        this.securityQuestionManagementEnabled = securityQuestionManagementEnabled;
    }

    public Map<String, Boolean> getExtensionsEnabled() {
        return extensionsEnabled;
    }

    public boolean isExtensionEnabled(final String key) {
        return extensionsEnabled.getOrDefault(key, true);
    }

}
