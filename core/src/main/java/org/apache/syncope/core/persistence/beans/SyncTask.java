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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.util.XMLSerializer;

@Entity
public class SyncTask extends AbstractSyncTask {

    private static final long serialVersionUID = -4141057723006682563L;

    @Lob
    private String userTemplate;

    @Lob
    private String roleTemplate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer fullReconciliation;

    /**
     * Default constructor.
     */
    public SyncTask() {
        super("org.apache.syncope.core.sync.impl.SyncJob");
    }

    public UserTO getUserTemplate() {
        return userTemplate == null
                ? new UserTO()
                : XMLSerializer.<UserTO>deserialize(userTemplate);
    }

    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = XMLSerializer.serialize(userTemplate);
    }

    public RoleTO getRoleTemplate() {
        return userTemplate == null
                ? new RoleTO()
                : XMLSerializer.<RoleTO>deserialize(roleTemplate);
    }

    public void setRoleTemplate(final RoleTO roleTemplate) {
        this.roleTemplate = XMLSerializer.serialize(roleTemplate);
    }

    public boolean isFullReconciliation() {
        return isBooleanAsInteger(fullReconciliation);
    }

    public void setFullReconciliation(final boolean fullReconciliation) {
        this.fullReconciliation = getBooleanAsInteger(fullReconciliation);
    }
}
