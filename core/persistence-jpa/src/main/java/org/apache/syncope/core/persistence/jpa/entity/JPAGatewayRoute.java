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
package org.apache.syncope.core.persistence.jpa.entity;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.GatewayRouteFilter;
import org.apache.syncope.common.lib.types.GatewayRoutePredicate;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;
import org.apache.syncope.core.persistence.api.entity.GatewayRoute;
import org.apache.syncope.core.persistence.jpa.validation.entity.GatewayRouteCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAGatewayRoute.TABLE)
@GatewayRouteCheck
public class JPAGatewayRoute extends AbstractGeneratedKeyEntity implements GatewayRoute {

    private static final long serialVersionUID = -8718852361106840530L;

    public static final String TABLE = "GatewayRoute";

    @Column(unique = true, nullable = false)
    private String name;

    private Integer routeOrder;

    @NotNull
    private String target;

    @Lob
    private String predicates;

    @Lob
    private String filters;

    @NotNull
    @Enumerated(EnumType.STRING)
    private GatewayRouteStatus status;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
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
    public URI getTarget() {
        return URI.create(target);
    }

    @Override
    public void setTarget(final URI target) {
        this.target = Optional.ofNullable(target).map(URI::toASCIIString).orElse(null);
    }

    @Override
    public List<GatewayRouteFilter> getFilters() {
        return filters == null
                ? List.of()
                : List.of(POJOHelper.deserialize(filters, GatewayRouteFilter[].class));
    }

    @Override
    public void setFilters(final List<GatewayRouteFilter> filters) {
        this.filters = POJOHelper.serialize(filters);
    }

    @Override
    public List<GatewayRoutePredicate> getPredicates() {
        return predicates == null
                ? List.of()
                : List.of(POJOHelper.deserialize(predicates, GatewayRoutePredicate[].class));
    }

    @Override
    public void setPredicates(final List<GatewayRoutePredicate> predicates) {
        this.predicates = POJOHelper.serialize(predicates);
    }

    @Override
    public GatewayRouteStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(final GatewayRouteStatus status) {
        this.status = status;
    }
}
