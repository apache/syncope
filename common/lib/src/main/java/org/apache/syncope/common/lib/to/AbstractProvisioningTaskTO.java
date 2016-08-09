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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;

@XmlRootElement(name = "abstractProvisioningTask")
@XmlType
@XmlSeeAlso({ PushTaskTO.class, PullTaskTO.class })
public class AbstractProvisioningTaskTO extends SchedTaskTO {

    private static final long serialVersionUID = -2143537546915809016L;

    private String resource;

    private boolean performCreate;

    private boolean performUpdate;

    private boolean performDelete;

    private boolean syncStatus;

    private UnmatchingRule unmatchingRule;

    private MatchingRule matchingRule;

    private final Set<String> actionsClassNames = new HashSet<>();

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public boolean isPerformCreate() {
        return performCreate;
    }

    public void setPerformCreate(final boolean performCreate) {
        this.performCreate = performCreate;
    }

    public boolean isPerformUpdate() {
        return performUpdate;
    }

    public void setPerformUpdate(final boolean performUpdate) {
        this.performUpdate = performUpdate;
    }

    public boolean isPerformDelete() {
        return performDelete;
    }

    public void setPerformDelete(final boolean performDelete) {
        this.performDelete = performDelete;
    }

    public boolean isSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(final boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    @XmlElementWrapper(name = "actionsClassNames")
    @XmlElement(name = "actionsClassName")
    @JsonProperty("actionsClassNames")
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
    }

    public UnmatchingRule getUnmatchingRule() {
        return unmatchingRule;
    }

    public void setUnmatchingRule(final UnmatchingRule unmatchigRule) {
        this.unmatchingRule = unmatchigRule;
    }

    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(final MatchingRule matchigRule) {
        this.matchingRule = matchigRule;
    }
}
