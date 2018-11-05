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
package org.apache.syncope.core.persistence.api.dao.search;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class AssignableCond extends AbstractSearchCond {

    private static final long serialVersionUID = 1237627275756159522L;

    private String realmFullPath;

    /**
     * Whether this condition should be evaluated from the assignable group's or instead the
     * assignee's point of view (default).
     * The converter from FIQL will ignore this setting, which is meant for internal usage.
     */
    private boolean fromGroup = false;

    public String getRealmFullPath() {
        return realmFullPath;
    }

    public void setRealmFullPath(final String realmFullPath) {
        this.realmFullPath = realmFullPath;
    }

    public boolean isFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(final boolean fromGroup) {
        this.fromGroup = fromGroup;
    }

    @Override
    public final boolean isValid() {
        return realmFullPath != null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(realmFullPath).
                append(fromGroup).
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
        final AssignableCond other = (AssignableCond) obj;
        return new EqualsBuilder().
                append(realmFullPath, other.realmFullPath).
                append(fromGroup, other.fromGroup).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(realmFullPath).
                append(fromGroup).
                build();
    }
}
