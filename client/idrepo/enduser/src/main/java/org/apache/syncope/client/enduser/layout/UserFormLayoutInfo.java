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

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.enduser.panels.UserFormPanel;
import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormBaseLayout;
import org.apache.syncope.client.ui.commons.layout.UserForm;
import org.apache.syncope.common.lib.to.UserTO;

public class UserFormLayoutInfo extends AbstractAnyFormBaseLayout<UserTO, UserForm> {

    private static final long serialVersionUID = -5573691733739618500L;

    private final Map<String, CustomizationOption> whichPlainAttrs = new HashMap<>();

    private final Map<String, CustomizationOption> whichDerAttrs = new HashMap<>();

    private final Map<String, CustomizationOption> whichVirAttrs = new HashMap<>();

    private boolean passwordManagement = true;

    private boolean detailsManagement = true;

    private final SidebarLayout sidebarLayout;

    public UserFormLayoutInfo() {
        sidebarLayout = new SidebarLayout();
    }

    public Map<String, CustomizationOption> getWhichPlainAttrs() {
        return whichPlainAttrs;
    }

    public Map<String, CustomizationOption> getWhichDerAttrs() {
        return whichDerAttrs;
    }

    public Map<String, CustomizationOption> getWhichVirAttrs() {
        return whichVirAttrs;
    }

    @Override
    protected Class<? extends UserForm> getDefaultFormClass() {
        return UserFormPanel.class;
    }

    public boolean isPasswordManagement() {
        return passwordManagement;
    }

    public void setPasswordManagement(final boolean passwordManagement) {
        this.passwordManagement = passwordManagement;
    }

    public boolean isDetailsManagement() {
        return detailsManagement;
    }

    public void setDetailsManagement(final boolean detailsManagement) {
        this.detailsManagement = detailsManagement;
    }

    public SidebarLayout getSidebarLayout() {
        return sidebarLayout;
    }

}
