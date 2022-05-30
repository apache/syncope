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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import javax.ws.rs.QueryParam;

public class U2FDeviceQuery extends AbstractQuery {

    private static final long serialVersionUID = -7381828286332101171L;

    public static class Builder extends AbstractQuery.Builder<U2FDeviceQuery, U2FDeviceQuery.Builder> {

        @Override
        protected U2FDeviceQuery newInstance() {
            return new U2FDeviceQuery();
        }

        public U2FDeviceQuery.Builder owner(final String owner) {
            getInstance().setOwner(owner);
            return this;
        }

        public U2FDeviceQuery.Builder id(final Long id) {
            getInstance().setId(id);
            return this;
        }

        public U2FDeviceQuery.Builder expirationDate(final OffsetDateTime date) {
            getInstance().setExpirationDate(date);
            return this;
        }
    }

    private Long id;

    private OffsetDateTime expirationDate;

    private String owner;

    @Parameter(name = "id", in = ParameterIn.QUERY, schema =
            @Schema(implementation = Long.class))
    public Long getId() {
        return id;
    }

    @QueryParam("id")
    public void setId(final Long id) {
        this.id = id;
    }

    @Parameter(name = "expirationDate", in = ParameterIn.QUERY, schema =
            @Schema(implementation = OffsetDateTime.class))
    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    @QueryParam("expirationDate")
    public void setExpirationDate(final OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Parameter(name = "owner", in = ParameterIn.QUERY, schema =
            @Schema(implementation = String.class))
    public String getOwner() {
        return owner;
    }

    @QueryParam("owner")
    public void setOwner(final String owner) {
        this.owner = owner;
    }
}
