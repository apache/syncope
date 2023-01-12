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

import jakarta.ws.rs.PathParam;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARouteType;

public class SRARouteTO implements NamedEntityTO {

    private static final long serialVersionUID = 4044528284951757870L;

    private String key;

    private String name;

    private URI target;

    private URI error;

    private SRARouteType type = SRARouteType.PUBLIC;

    private boolean logout = false;

    private URI postLogout;

    private boolean csrf = true;

    private int order = 0;

    private final List<SRARouteFilter> filters = new ArrayList<>();

    private final List<SRARoutePredicate> predicates = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public URI getTarget() {
        return target;
    }

    public void setTarget(final URI target) {
        this.target = target;
    }

    public URI getError() {
        return error;
    }

    public void setError(final URI error) {
        this.error = error;
    }

    public SRARouteType getType() {
        return type;
    }

    public void setType(final SRARouteType type) {
        this.type = type;
    }

    public boolean isLogout() {
        return logout;
    }

    public void setLogout(final boolean logout) {
        this.logout = logout;
    }

    public URI getPostLogout() {
        return postLogout;
    }

    public void setPostLogout(final URI postLogout) {
        this.postLogout = postLogout;
    }

    public boolean isCsrf() {
        return csrf;
    }

    public void setCsrf(final boolean csrf) {
        this.csrf = csrf;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public List<SRARouteFilter> getFilters() {
        return filters;
    }

    public List<SRARoutePredicate> getPredicates() {
        return predicates;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(target).
                append(error).
                append(type).
                append(logout).
                append(postLogout).
                append(csrf).
                append(order).
                append(filters).
                append(predicates).
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
        final SRARouteTO other = (SRARouteTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(target, other.target).
                append(error, other.error).
                append(type, other.type).
                append(logout, other.logout).
                append(postLogout, other.postLogout).
                append(csrf, other.csrf).
                append(order, other.order).
                append(filters, other.filters).
                append(predicates, other.predicates).
                build();
    }
}
