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
package org.apache.syncope.client.console.layout;

import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.ui.commons.layout.UserForm;
import org.apache.syncope.common.lib.to.UserTO;

public class UserFormLayoutInfo extends AbstractAnyFormLayout<UserTO, UserForm> {

    private static final long serialVersionUID = -5573691733739618500L;

    private boolean passwordManagement = true;

    private boolean roles = true;

    private boolean relationships = true;

    @Override
    protected Class<? extends UserForm> getDefaultFormClass() {
        return UserWizardBuilder.class;
    }

    public boolean isPasswordManagement() {
        return passwordManagement;
    }

    public void setPasswordManagement(final boolean passwordManagement) {
        this.passwordManagement = passwordManagement;
    }

    public boolean isRoles() {
        return roles;
    }

    public void setRoles(final boolean roles) {
        this.roles = roles;
    }

    public boolean isRelationships() {
        return relationships;
    }

    public void setRelationships(final boolean relationships) {
        this.relationships = relationships;
    }
}
