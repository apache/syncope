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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OrgUnit extends ItemContainer {

    private static final long serialVersionUID = -1868877794174953177L;

    private String objectClass;

    private String connObjectLink;

    private String syncToken;

    private boolean ignoreCaseMatch;

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(final String objectClass) {
        this.objectClass = objectClass;
    }

    public String getConnObjectLink() {
        return connObjectLink;
    }

    public void setConnObjectLink(final String connObjectLink) {
        this.connObjectLink = connObjectLink;
    }

    public String getSyncToken() {
        return syncToken;
    }

    public void setSyncToken(final String syncToken) {
        this.syncToken = syncToken;
    }

    public boolean isIgnoreCaseMatch() {
        return ignoreCaseMatch;
    }

    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
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
        OrgUnit other = (OrgUnit) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(ignoreCaseMatch, other.ignoreCaseMatch).
                append(objectClass, other.objectClass).
                append(connObjectLink, other.connObjectLink).
                append(syncToken, other.syncToken).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(objectClass).
                append(connObjectLink).
                append(syncToken).
                append(ignoreCaseMatch).
                build();
    }
}
