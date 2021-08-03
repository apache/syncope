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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key" })
@Schema(
        subTypes = { PropagationTaskTO.class, ProvisioningTaskTO.class, NotificationTaskTO.class },
        discriminatorProperty = "_class")
public abstract class TaskTO extends AbstractStartEndBean implements EntityTO {

    private static final long serialVersionUID = 386450127003321197L;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    private String key;

    private String latestExecStatus;

    private String lastExecutor;

    private final List<ExecTO> executions = new ArrayList<>();

    @Schema(name = "_class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getLatestExecStatus() {
        return latestExecStatus;
    }

    public void setLatestExecStatus(final String latestExecStatus) {
        this.latestExecStatus = latestExecStatus;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getLastExecutor() {
        return lastExecutor;
    }

    public void setLastExecutor(final String lastExecutor) {
        this.lastExecutor = lastExecutor;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JacksonXmlElementWrapper(localName = "executions")
    @JacksonXmlProperty(localName = "execution")
    public List<ExecTO> getExecutions() {
        return executions;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(discriminator).
                append(executions).
                append(latestExecStatus).
                append(lastExecutor).
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
        final TaskTO other = (TaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(discriminator, other.discriminator).
                append(executions, other.executions).
                append(latestExecStatus, other.latestExecStatus).
                append(lastExecutor, other.lastExecutor).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                appendSuper(super.toString()).
                append(key).
                append(discriminator).
                append(executions).
                append(latestExecStatus).
                append(lastExecutor).
                build();
    }
}
