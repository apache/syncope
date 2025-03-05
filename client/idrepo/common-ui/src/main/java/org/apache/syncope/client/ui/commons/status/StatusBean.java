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
package org.apache.syncope.client.ui.commons.status;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;

public class StatusBean implements Serializable {

    private static final long serialVersionUID = -5207260204921071129L;

    private final String key;

    private final String name;

    private final String resource;

    private String connObjectLink;

    private Status status = Status.OBJECT_NOT_FOUND;

    private boolean linked = true;

    public StatusBean(final AnyTO any, final String resource) {
        this.key = any.getKey();
        this.name = any instanceof final UserTO userTO
                ? userTO.getUsername()
                : any instanceof final GroupTO groupTO
                        ? groupTO.getName()
                        : ((AnyObjectTO) any).getName();
        this.resource = resource;
    }

    public StatusBean(final RealmTO realm, final String resource) {
        this.key = realm.getKey();
        this.name = realm.getFullPath();
        this.resource = resource;
    }

    public String getConnObjectLink() {
        return connObjectLink;
    }

    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    public String getResource() {
        return resource;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(final boolean linked) {
        this.linked = linked;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(resource).
                append(connObjectLink).
                append(status).
                append(linked).
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
        final StatusBean other = (StatusBean) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(resource, other.resource).
                append(connObjectLink, other.connObjectLink).
                append(status, other.status).
                append(linked, other.linked).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(key).
                append(name).
                append(resource).
                append(connObjectLink).
                append(status).
                append(linked).
                build();
    }
}
