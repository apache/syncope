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

public class AuxClassCond extends AbstractSearchCond {

    private static final long serialVersionUID = 4298076973281246633L;

    private String auxClass;

    public String getAuxClass() {
        return auxClass;
    }

    public void setAuxClass(final String auxClass) {
        this.auxClass = auxClass;
    }

    @Override
    public boolean isValid() {
        return auxClass != null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(auxClass).
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
        final AuxClassCond other = (AuxClassCond) obj;
        return new EqualsBuilder().
                append(auxClass, other.auxClass).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(auxClass).
                build();
    }
}
