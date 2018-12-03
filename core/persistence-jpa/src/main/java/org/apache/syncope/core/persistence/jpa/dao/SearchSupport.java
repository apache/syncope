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
package org.apache.syncope.core.persistence.jpa.dao;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

public class SearchSupport {

    public static class SearchView {

        protected String alias;

        protected String name;

        protected SearchView(final String alias, final String name) {
            this.alias = alias;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    append(alias).
                    append(name).
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
            final SearchView other = (SearchView) obj;
            return new EqualsBuilder().
                    append(alias, other.alias).
                    append(name, other.name).
                    build();
        }
    }

    protected final AnyTypeKind anyTypeKind;

    protected boolean nonMandatorySchemas = false;

    public SearchSupport(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public SearchView table() {
        String result;

        switch (anyTypeKind) {
            case ANY_OBJECT:
                result = JPAAnyObject.TABLE;
                break;

            case GROUP:
                result = JPAGroup.TABLE;
                break;

            case USER:
            default:
                result = JPAUser.TABLE;
                break;
        }

        return new SearchView("t", result);
    }

    public SearchView field() {
        String result;

        switch (anyTypeKind) {
            case ANY_OBJECT:
                result = "anyObject_search";
                break;

            case GROUP:
                result = "group_search";
                break;

            case USER:
            default:
                result = "user_search";
                break;
        }

        return new SearchView("sv", result);
    }

    public SearchView relationship() {
        String kind = anyTypeKind == AnyTypeKind.USER ? "u" : "a";
        return new SearchView("sv" + kind + "m", field().name + "_" + kind + "relationship");
    }

    public SearchView membership() {
        String kind = anyTypeKind == AnyTypeKind.USER ? "u" : "a";
        return new SearchView("sv" + kind + "m", field().name + "_" + kind + "membership");
    }

    public SearchView dyngroupmembership() {
        return new SearchView("sv" + anyTypeKind.name() + "dgm",
                anyTypeKind == AnyTypeKind.USER ? JPAGroupDAO.UDYNMEMB_TABLE : JPAGroupDAO.ADYNMEMB_TABLE);
    }

    public SearchView role() {
        return new SearchView("svr", field().name + "_role");
    }

    public SearchView priv() {
        return new SearchView("svp", field().name + "_priv");
    }

    public SearchView dynpriv() {
        return new SearchView("svdp", field().name + "_dynpriv");
    }

    public SearchView dynrolemembership() {
        return new SearchView("svdr", JPARoleDAO.DYNMEMB_TABLE);
    }

    public SearchView dynrealmmembership() {
        return new SearchView("svdrealm", JPADynRealmDAO.DYNMEMB_TABLE);
    }

    public SearchView resource() {
        return new SearchView("svr", field().name + "_resource");
    }

    public SearchView groupResource() {
        return new SearchView("svrr", field().name + "_group_res");
    }

    public SearchView entitlements() {
        return new SearchView("sve", field().name + "_entitlements");
    }

    SearchViewSupport asSearchViewSupport() {
        if (this instanceof SearchViewSupport) {
            return (SearchViewSupport) this;
        }
        throw new IllegalArgumentException("Not an " + SearchViewSupport.class + " instance");
    }
}
