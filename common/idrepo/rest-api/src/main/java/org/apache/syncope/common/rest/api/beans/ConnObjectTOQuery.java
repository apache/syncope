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
package org.apache.syncope.common.rest.api.beans;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class ConnObjectTOQuery implements Serializable {

    private static final long serialVersionUID = -371488230250055359L;

    private static final int MAX_SIZE = 100;

    public static class Builder {

        private final ConnObjectTOQuery instance = new ConnObjectTOQuery();

        public Builder size(final Integer size) {
            instance.setSize(size);
            return this;
        }

        public Builder pagedResultsCookie(final String pagedResultsCookie) {
            instance.setPagedResultsCookie(pagedResultsCookie);
            return this;
        }

        public Builder orderBy(final String orderBy) {
            instance.setOrderBy(orderBy);
            return this;
        }

        public Builder fiql(final String fiql) {
            instance.setFiql(fiql);
            return this;
        }

        public Builder moreAttrsToGet(final String... moreAttrsToGet) {
            if (moreAttrsToGet != null) {
                Set<String> matg = Optional.ofNullable(instance.getMoreAttrsToGet()).orElseGet(HashSet::new);
                matg.addAll(Stream.of(moreAttrsToGet).collect(Collectors.toSet()));
                instance.setMoreAttrsToGet(matg);
            }
            return this;
        }

        public Builder moreAttrsToGet(final Collection<String> moreAttrsToGet) {
            if (moreAttrsToGet != null) {
                Set<String> matg = Optional.ofNullable(instance.getMoreAttrsToGet()).orElseGet(HashSet::new);
                matg.addAll(moreAttrsToGet);
                instance.setMoreAttrsToGet(matg);
            }
            return this;
        }

        public ConnObjectTOQuery build() {
            return instance;
        }
    }

    private Integer size;

    private String pagedResultsCookie;

    private String orderBy;

    private String fiql;

    private Set<String> moreAttrsToGet;

    public Integer getSize() {
        return size == null
                ? 25
                : size > MAX_SIZE
                        ? MAX_SIZE
                        : size;
    }

    @Min(1)
    @Max(MAX_SIZE)
    @QueryParam(JAXRSService.PARAM_SIZE)
    @DefaultValue("25")
    public void setSize(final Integer size) {
        this.size = size;
    }

    public String getPagedResultsCookie() {
        return pagedResultsCookie;
    }

    @QueryParam(JAXRSService.PARAM_CONNID_PAGED_RESULTS_COOKIE)
    public void setPagedResultsCookie(final String pagedResultsCookie) {
        this.pagedResultsCookie = pagedResultsCookie;
    }

    @QueryParam(JAXRSService.PARAM_ORDERBY)
    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
    }

    public String getFiql() {
        return fiql;
    }

    @QueryParam(JAXRSService.PARAM_FIQL)
    public void setFiql(final String fiql) {
        this.fiql = fiql;
    }

    public Set<String> getMoreAttrsToGet() {
        return moreAttrsToGet;
    }

    @QueryParam("moreAttrsToGet")
    public void setMoreAttrsToGet(final Set<String> moreAttrsToGet) {
        this.moreAttrsToGet = moreAttrsToGet;
    }
}
