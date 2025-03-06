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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.syncope.core.persistence.api.entity.SRARoute;
import org.apache.syncope.core.persistence.common.validation.SRARouteCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jSRARoute.NODE)
@SRARouteCheck
public class Neo4jSRARoute extends AbstractGeneratedKeyNode implements SRARoute {

    private static final long serialVersionUID = -8718852361106840530L;

    public static final String NODE = "SRARoute";

    @NotNull
    private String name;

    @NotNull
    private String target;

    private String error;

    @NotNull
    private SRARouteType routeType;

    @NotNull
    private Boolean logout = false;

    private String postLogout;

    @NotNull
    private Boolean csrf = true;

    private Integer routeOrder;

    private String predicates;

    private String filters;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public URI getTarget() {
        return Optional.ofNullable(target).map(URI::create).orElse(null);
    }

    @Override
    public void setTarget(final URI target) {
        this.target = Optional.ofNullable(target).map(URI::toASCIIString).orElse(null);
    }

    @Override
    public URI getError() {
        return Optional.ofNullable(error).map(URI::create).orElse(null);
    }

    @Override
    public void setError(final URI error) {
        this.error = Optional.ofNullable(error).map(URI::toASCIIString).orElse(null);
    }

    @Override
    public SRARouteType getType() {
        return routeType;
    }

    @Override
    public void setType(final SRARouteType type) {
        this.routeType = type;
    }

    @Override
    public boolean isLogout() {
        return BooleanUtils.isNotFalse(logout);
    }

    @Override
    public void setLogout(final boolean logout) {
        this.logout = logout;
    }

    @Override
    public URI getPostLogout() {
        return Optional.ofNullable(postLogout).map(URI::create).orElse(null);
    }

    @Override
    public void setPostLogout(final URI postLogout) {
        this.postLogout = Optional.ofNullable(postLogout).map(URI::toASCIIString).orElse(null);
    }

    @Override
    public boolean isCsrf() {
        return csrf;
    }

    @Override
    public void setCsrf(final boolean csrf) {
        this.csrf = csrf;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(routeOrder).orElse(0);
    }

    @Override
    public void setOrder(final int order) {
        this.routeOrder = order;
    }

    @Override
    public List<SRARouteFilter> getFilters() {
        return Optional.ofNullable(filters).
                map(f -> List.of(POJOHelper.deserialize(f, SRARouteFilter[].class))).
            orElseGet(List::of);
    }

    @Override
    public void setFilters(final List<SRARouteFilter> filters) {
        this.filters = POJOHelper.serialize(filters);
    }

    @Override
    public List<SRARoutePredicate> getPredicates() {
        return Optional.ofNullable(predicates).
                map(f -> List.of(POJOHelper.deserialize(f, SRARoutePredicate[].class))).
            orElseGet(List::of);
    }

    @Override
    public void setPredicates(final List<SRARoutePredicate> predicates) {
        this.predicates = POJOHelper.serialize(predicates);
    }
}
