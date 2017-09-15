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

import java.io.Serializable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class ConnObjectTOListQuery implements Serializable {

    private static final long serialVersionUID = -371488230250055359L;

    private static final int MAX_SIZE = 100;

    public static class Builder {

        private final ConnObjectTOListQuery instance = new ConnObjectTOListQuery();

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

        public ConnObjectTOListQuery build() {
            return instance;
        }

    }

    private Integer size;

    private String pagedResultsCookie;

    private String orderBy;

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

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
