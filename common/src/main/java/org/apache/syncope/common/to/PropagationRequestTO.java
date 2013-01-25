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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

/**
 * Propagation request on internal storage or on 0+ external resources.
 */
@XmlRootElement(name = "propagationRequest")
@XmlType
public class PropagationRequestTO extends AbstractBaseBean {

    private static final long serialVersionUID = 7601716025754543004L;

    /**
     * External resources propagation is requested to.
     */
    private final Set<String> resources;

    /**
     * Whether update should be performed on internal storage.
     */
    private boolean onSyncope;

    public PropagationRequestTO() {
        super();

        this.resources = new HashSet<String>();
    }

    public boolean isOnSyncope() {
        return onSyncope;
    }

    public void setOnSyncope(final boolean onSyncope) {
        this.onSyncope = onSyncope;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    public Set<String> getResources() {
        return resources;
    }

    public boolean addResource(final String resource) {
        return this.resources.add(resource);
    }

    public boolean removeResource(final String resource) {
        return this.resources.remove(resource);
    }

    public void setResources(final Set<String> resources) {
        if (this.resources != resources) {
            this.resources.clear();
            if (resources != null && !resources.isEmpty()) {
                this.resources.addAll(resources);
            }
        }
    }
}
