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

import java.io.Serializable;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OrderByClause implements Serializable {

    private static final long serialVersionUID = -1741826744085524716L;

    public enum Direction {

        ASC,
        DESC

    }

    private String field;

    private Direction direction;

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public Direction getDirection() {
        return Optional.ofNullable(direction).orElse(Direction.ASC);
    }

    public void setDirection(final Direction direction) {
        this.direction = direction;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(field).
                append(direction).
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
        final OrderByClause other = (OrderByClause) obj;
        return new EqualsBuilder().
                append(field, other.field).
                append(direction, other.direction).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(field).
                append(direction).
                build();
    }
}
