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
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { ProvisioningTaskTO.class },
        subTypes = { PullTaskTO.class, LiveSyncTaskTO.class }, discriminatorProperty = "_class")
public abstract class InboundTaskTO extends ProvisioningTaskTO {

    private static final long serialVersionUID = -5538466397907585166L;

    @JsonProperty(required = true)
    private String destinationRealm;

    private boolean remediation;

    public String getDestinationRealm() {
        return destinationRealm;
    }

    public void setDestinationRealm(final String destinationRealm) {
        this.destinationRealm = destinationRealm;
    }

    public boolean isRemediation() {
        return remediation;
    }

    public void setRemediation(final boolean remediation) {
        this.remediation = remediation;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(destinationRealm).
                append(remediation).
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
        final InboundTaskTO other = (InboundTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(destinationRealm, other.destinationRealm).
                append(remediation, other.remediation).
                build();
    }
}
