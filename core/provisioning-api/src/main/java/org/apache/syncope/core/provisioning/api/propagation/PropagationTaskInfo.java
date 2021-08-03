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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.objects.ConnectorObject;

@SuppressWarnings("squid:S1948")
public class PropagationTaskInfo extends PropagationTaskTO {

    private static final long serialVersionUID = -2879861567335503099L;

    private final ExternalResource externalResource;

    private Connector connector;

    /**
     * Object on External Resource before propagation takes place.
     *
     * null: beforeObj was not attempted to read
     * not null but not present: beforeObj was attempted to read, but not found
     * not null and present: beforeObj value is available
     */
    private Optional<ConnectorObject> beforeObj;

    public PropagationTaskInfo(final ExternalResource externalResource) {
        super();
        this.externalResource = externalResource;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(final Connector connector) {
        this.connector = connector;
    }

    public ExternalResource getExternalResource() {
        return externalResource;
    }

    @Override
    public String getResource() {
        return externalResource.getKey();
    }

    @Override
    public void setResource(final String resource) {
        throw new IllegalArgumentException("Cannot set ExternalResource on " + getClass().getName());
    }

    public Optional<ConnectorObject> getBeforeObj() {
        return beforeObj;
    }

    public void setBeforeObj(final Optional<ConnectorObject> beforeObj) {
        this.beforeObj = beforeObj;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(externalResource.getKey()).
                append(beforeObj).
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
        final PropagationTaskInfo other = (PropagationTaskInfo) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(externalResource.getKey(), other.externalResource.getKey()).
                append(beforeObj, other.beforeObj).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                appendSuper(super.toString()).
                append(externalResource).
                append(beforeObj).
                build();
    }
}
