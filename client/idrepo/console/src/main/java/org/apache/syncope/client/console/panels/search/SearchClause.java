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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public final class SearchClause implements Serializable {

    private static final long serialVersionUID = 2010794463096110104L;

    public enum Operator {

        AND,
        OR;

    }

    public enum Type {

        ATTRIBUTE,
        GROUP_MEMBERSHIP,
        GROUP_MEMBER,
        ROLE_MEMBERSHIP,
        AUX_CLASS,
        RESOURCE,
        RELATIONSHIP,
        CUSTOM;

    }

    public enum Comparator {

        IS_NULL,
        IS_NOT_NULL,
        EQUALS,
        NOT_EQUALS,
        GREATER_OR_EQUALS,
        GREATER_THAN,
        LESS_OR_EQUALS,
        LESS_THAN;

    }

    private Operator operator;

    private Type type;

    private String property;

    private Comparator comparator;

    private String value;

    public SearchClause() {
        setOperator(SearchClause.Operator.AND);
        setComparator(SearchClause.Comparator.EQUALS);
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(final Operator operator) {
        this.operator = operator;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(final String property) {
        this.property = property;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setComparator(final Comparator comparator) {
        this.comparator = comparator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(operator).
                append(type).
                append(property).
                append(comparator).
                append(value).
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
        final SearchClause other = (SearchClause) obj;
        return new EqualsBuilder().
                append(operator, other.operator).
                append(type, other.type).
                append(property, other.property).
                append(comparator, other.comparator).
                append(value, other.value).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(operator).
                append(type).
                append(property).
                append(comparator).
                append(value).
                build();
    }
}
