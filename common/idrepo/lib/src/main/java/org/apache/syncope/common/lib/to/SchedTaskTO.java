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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { TaskTO.class },
        subTypes = { ProvisioningTaskTO.class, MacroTaskTO.class },
        discriminatorProperty = "_class")
public class SchedTaskTO extends TaskTO implements NamedEntityTO {

    private static final long serialVersionUID = -5722284116974636425L;

    private OffsetDateTime startAt;

    private String cronExpression;

    private String jobDelegate;

    private String name;

    private String description;

    private OffsetDateTime lastExec;

    private OffsetDateTime nextExec;

    private boolean active = true;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.SchedTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(final OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobDelegate() {
        return jobDelegate;
    }

    public void setJobDelegate(final String jobDelegate) {
        this.jobDelegate = jobDelegate;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getLastExec() {
        return lastExec;
    }

    public void setLastExec(final OffsetDateTime lastExec) {
        this.lastExec = lastExec;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getNextExec() {
        return nextExec;
    }

    public void setNextExec(final OffsetDateTime nextExec) {
        this.nextExec = nextExec;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @JsonProperty(required = true)
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(startAt).
                append(cronExpression).
                append(jobDelegate).
                append(name).
                append(description).
                append(lastExec).
                append(nextExec).
                append(active).
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
        final SchedTaskTO other = (SchedTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(startAt, other.startAt).
                append(cronExpression, other.cronExpression).
                append(jobDelegate, other.jobDelegate).
                append(name, other.name).
                append(description, other.description).
                append(lastExec, other.lastExec).
                append(nextExec, other.nextExec).
                append(active, other.active).
                build();
    }
}
