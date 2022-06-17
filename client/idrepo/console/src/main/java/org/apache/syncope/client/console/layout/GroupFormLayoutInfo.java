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

import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormLayout;
import org.apache.syncope.common.lib.to.GroupTO;

public class GroupFormLayoutInfo extends AbstractAnyFormLayout<GroupTO, GroupForm> {

    private static final long serialVersionUID = -5573691733739618500L;

    private boolean ownership = true;

    private boolean dynamicMemberships = true;

    private boolean typeExtensions = true;

    @Override
    protected Class<? extends GroupForm> getDefaultFormClass() {
        return GroupWizardBuilder.class;
    }

    public boolean isOwnership() {
        return ownership;
    }

    public void setOwnership(final boolean ownership) {
        this.ownership = ownership;
    }

    public boolean isDynamicMemberships() {
        return dynamicMemberships;
    }

    public void setDynamicMemberships(final boolean dynamicMemberships) {
        this.dynamicMemberships = dynamicMemberships;
    }

    public boolean isTypeExtensions() {
        return typeExtensions;
    }

    public void setTypeExtensions(final boolean typeExtensions) {
        this.typeExtensions = typeExtensions;
    }

}
