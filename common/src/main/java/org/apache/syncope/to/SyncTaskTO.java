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
package org.apache.syncope.to;

public class SyncTaskTO extends SchedTaskTO {

    private static final long serialVersionUID = -2143537546915809016L;

    private String resource;

    private UserTO userTemplate;

    private boolean performCreate;

    private boolean performUpdate;

    private boolean performDelete;

    private boolean syncStatus;

    private boolean fullReconciliation;

    private String actionsClassName;

    public UserTO getUserTemplate() {
        return userTemplate;
    }

    public void setUserTemplate(UserTO userTemplate) {
        this.userTemplate = userTemplate;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isPerformCreate() {
        return performCreate;
    }

    public void setPerformCreate(boolean performCreate) {
        this.performCreate = performCreate;
    }

    public boolean isPerformUpdate() {
        return performUpdate;
    }

    public void setPerformUpdate(boolean performUpdate) {
        this.performUpdate = performUpdate;
    }

    public boolean isPerformDelete() {
        return performDelete;
    }

    public void setPerformDelete(boolean performDelete) {
        this.performDelete = performDelete;
    }

    public boolean isSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    public boolean isFullReconciliation() {
        return fullReconciliation;
    }

    public void setFullReconciliation(boolean fullReconciliation) {
        this.fullReconciliation = fullReconciliation;
    }

    public String getActionsClassName() {
        return actionsClassName;
    }

    public void setActionsClassName(String actionsClassName) {
        this.actionsClassName = actionsClassName;
    }
}
