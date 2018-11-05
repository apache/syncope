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

public class AnyTypeCond extends AbstractSearchCond {

    private static final long serialVersionUID = 4298076973281246633L;

    private String anyTypeKey;

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    @Override
    public boolean isValid() {
        return anyTypeKey != null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(anyTypeKey).
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
        final AnyTypeCond other = (AnyTypeCond) obj;
        return new EqualsBuilder().
                append(anyTypeKey, other.anyTypeKey).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(anyTypeKey).
                build();
    }
}
