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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;

@Schema(allOf = { SchedTaskTO.class },
        subTypes = { PushTaskTO.class, InboundTaskTO.class }, discriminatorProperty = "_class")
public abstract class ProvisioningTaskTO extends SchedTaskTO {

    private static final long serialVersionUID = -5722284116974636425L;

    private String resource;

    private boolean performCreate;

    private boolean performUpdate;

    private boolean performDelete;

    private boolean syncStatus;

    private UnmatchingRule unmatchingRule;

    private MatchingRule matchingRule;

    private final List<String> actions = new ArrayList<>();

    private ThreadPoolSettings concurrentSettings;

    @JsonProperty(required = true)
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

    @JacksonXmlElementWrapper(localName = "actions")
    @JacksonXmlProperty(localName = "action")
    public List<String> getActions() {
        return actions;
    }

    public ThreadPoolSettings getConcurrentSettings() {
        return concurrentSettings;
    }

    public void setConcurrentSettings(final ThreadPoolSettings concurrentSettings) {
        this.concurrentSettings = concurrentSettings;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(resource).
                append(performCreate).
                append(performUpdate).
                append(performDelete).
                append(syncStatus).
                append(unmatchingRule).
                append(matchingRule).
                append(actions).
                append(concurrentSettings).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProvisioningTaskTO other = (ProvisioningTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(resource, other.resource).
                append(performCreate, other.performCreate).
                append(performUpdate, other.performUpdate).
                append(performDelete, other.performDelete).
                append(syncStatus, other.syncStatus).
                append(unmatchingRule, other.unmatchingRule).
                append(matchingRule, other.matchingRule).
                append(actions, other.actions).
                append(concurrentSettings, other.concurrentSettings).
                build();
    }
}
