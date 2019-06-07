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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.GatewayRouteFilter;
import org.apache.syncope.common.lib.types.GatewayRoutePredicate;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;

@XmlRootElement(name = "gatewayRoute")
@XmlType
public class GatewayRouteTO implements NamedEntityTO {

    private static final long serialVersionUID = 4044528284951757870L;

    private String key;

    private String name;

    private int order = 0;

    private URI target;

    private final List<GatewayRouteFilter> filters = new ArrayList<>();

    private final List<GatewayRoutePredicate> predicates = new ArrayList<>();

    private GatewayRouteStatus status;

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

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public URI getTarget() {
        return target;
    }

    public void setTarget(final URI target) {
        this.target = target;
    }

    @XmlElementWrapper(name = "filters")
    @XmlElement(name = "filter")
    @JsonProperty("filters")
    public List<GatewayRouteFilter> getFilters() {
        return filters;
    }

    @XmlElementWrapper(name = "predicates")
    @XmlElement(name = "predicate")
    @JsonProperty("predicates")
    public List<GatewayRoutePredicate> getPredicates() {
        return predicates;
    }

    public GatewayRouteStatus getStatus() {
        return status;
    }

    public void setStatus(final GatewayRouteStatus status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(target).
                append(filters).
                append(predicates).
                append(status).
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
        final GatewayRouteTO other = (GatewayRouteTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(target, other.target).
                append(filters, other.filters).
                append(predicates, other.predicates).
                append(status, other.status).
                build();
    }
}
