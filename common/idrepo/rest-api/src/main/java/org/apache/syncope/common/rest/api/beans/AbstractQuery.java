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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public abstract class AbstractQuery implements Serializable {

    private static final long serialVersionUID = -371488230250055359L;

    protected abstract static class Builder<Q extends AbstractQuery, B extends Builder<Q, B>> {

        private Q instance;

        protected abstract Q newInstance();

        protected Q getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        public B page(final Integer page) {
            getInstance().setPage(page);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B size(final Integer size) {
            getInstance().setSize(size);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B orderBy(final String orderBy) {
            getInstance().setOrderBy(orderBy);
            return (B) this;
        }

        public Q build() {
            return getInstance();
        }
    }

    private Integer page;

    private Integer size;

    private String orderBy;

    @Parameter(name = JAXRSService.PARAM_PAGE, description = "page", schema =
            @Schema(minimum = "1", implementation = Integer.class, defaultValue = "1"))
    public Integer getPage() {
        return page < 1 ? 1 : page;
    }

    @Min(1)
    @QueryParam(JAXRSService.PARAM_PAGE)
    @DefaultValue("1")
    public void setPage(final Integer page) {
        this.page = page;
    }

    @Parameter(name = JAXRSService.PARAM_SIZE, description = "items per page", schema =
            @Schema(minimum = "1", implementation = Integer.class, defaultValue = "25"))
    public Integer getSize() {
        return size < 1 ? 1 : size;
    }

    @Min(1)
    @QueryParam(JAXRSService.PARAM_SIZE)
    @DefaultValue("25")
    public void setSize(final Integer size) {
        this.size = size;
    }

    @Parameter(name = JAXRSService.PARAM_ORDERBY, description = "sorting conditions", schema =
            @Schema(implementation = String.class, example = "key DESC"))
    public String getOrderBy() {
        return orderBy;
    }

    @QueryParam(JAXRSService.PARAM_ORDERBY)
    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
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
        AbstractQuery other = (AbstractQuery) obj;
        return new EqualsBuilder().
                append(page, other.page).
                append(size, other.size).
                append(orderBy, other.orderBy).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(page).
                append(size).
                append(orderBy).
                build();
    }
}
