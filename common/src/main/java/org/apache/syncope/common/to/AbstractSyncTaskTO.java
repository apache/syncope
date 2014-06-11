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
package org.apache.syncope.common.to;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.UnmatchingRule;

@XmlRootElement(name = "abstractSyncTask")
@XmlType
@XmlSeeAlso({ PushTaskTO.class, SyncTaskTO.class })
public class AbstractSyncTaskTO extends SchedTaskTO {

    private static final long serialVersionUID = -2143537546915809016L;

    private String resource;

    private boolean performCreate;

    private boolean performUpdate;

    private boolean performDelete;

    private boolean syncStatus;

    private String actionsClassName;

    private UnmatchingRule unmatchigRule;

    private MatchingRule matchigRule;

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

    public String getActionsClassName() {
        return actionsClassName;
    }

    public void setActionsClassName(final String actionsClassName) {
        this.actionsClassName = actionsClassName;
    }

    public UnmatchingRule getUnmatchigRule() {
        return unmatchigRule;
    }

    public void setUnmatchigRule(final UnmatchingRule unmatchigRule) {
        this.unmatchigRule = unmatchigRule;
    }

    public MatchingRule getMatchigRule() {
        return matchigRule;
    }

    public void setMatchigRule(final MatchingRule matchigRule) {
        this.matchigRule = matchigRule;
    }
}
