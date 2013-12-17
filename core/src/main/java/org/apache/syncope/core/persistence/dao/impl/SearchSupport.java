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
package org.apache.syncope.core.persistence.dao.impl;

import static org.apache.syncope.common.types.AttributeSchemaType.Double;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.util.AttributableUtil;

class SearchSupport {

    static class SearchView {

        protected String alias;

        protected String name;

        protected SearchView(final String alias, final String name) {
            this.alias = alias;
            this.name = name;
        }

        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private final AttributableType type;

    public SearchSupport(final AttributableUtil attrUtil) {
        this.type = attrUtil.getType();
    }

    public String fieldName(final AttributeSchemaType type) {
        String result;

        switch (type) {
            case Boolean:
                result = "booleanvalue";
                break;

            case Date:
                result = "datevalue";
                break;

            case Double:
                result = "doublevalue";
                break;

            case Long:
                result = "longvalue";
                break;

            case String:
            case Enum:
                result = "stringvalue";
                break;

            default:
                result = null;
        }

        return result;
    }

    public SearchView field() {
        String result = "";

        switch (type) {
            case USER:
                result = "user_search";
                break;

            case ROLE:
                result = "role_search";
                break;

            case MEMBERSHIP:
            default:
        }

        return new SearchView("sv", result);
    }

    public SearchView attr() {
        return new SearchView("sva", field().name + "_attr");
    }

    public SearchView membership() {
        return new SearchView("svm", field().name + "_membership");
    }

    public SearchView nullAttr() {
        return new SearchView("svna", field().name + "_null_attr");
    }

    public SearchView resource() {
        return new SearchView("svr", field().name + "_resource");
    }

    public SearchView roleResource() {
        return new SearchView("svrr", field().name + "_role_resource");
    }

    public SearchView uniqueAttr() {
        return new SearchView("svua", field().name + "_unique_attr");
    }

    public SearchView entitlements() {
        return new SearchView("sve", field().name + "_entitlements");
    }
}
