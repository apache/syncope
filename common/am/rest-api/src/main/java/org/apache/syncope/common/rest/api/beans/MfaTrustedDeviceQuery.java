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
import jakarta.ws.rs.QueryParam;
import java.time.OffsetDateTime;

public class MfaTrustedDeviceQuery extends AbstractQuery {

    private static final long serialVersionUID = -7381828286332101171L;

    public static class Builder extends AbstractQuery.Builder<MfaTrustedDeviceQuery, MfaTrustedDeviceQuery.Builder> {

        @Override
        protected MfaTrustedDeviceQuery newInstance() {
            return new MfaTrustedDeviceQuery();
        }

        public MfaTrustedDeviceQuery.Builder id(final Long id) {
            getInstance().setId(id);
            return this;
        }

        public MfaTrustedDeviceQuery.Builder recordKey(final String recordKey) {
            getInstance().setRecordKey(recordKey);
            return this;
        }

        public MfaTrustedDeviceQuery.Builder principal(final String principal) {
            getInstance().setPrincipal(principal);
            return this;
        }

        public MfaTrustedDeviceQuery.Builder expirationDate(final OffsetDateTime date) {
            getInstance().setExpirationDate(date);
            return this;
        }

        public MfaTrustedDeviceQuery.Builder recordDate(final OffsetDateTime date) {
            getInstance().setRecordDate(date);
            return this;
        }
    }

    private Long id;

    private String recordKey;

    private OffsetDateTime expirationDate;

    private OffsetDateTime recordDate;

    private String principal;

    @Parameter(name = "id", in = ParameterIn.QUERY, schema =
            @Schema(implementation = Long.class))
    public Long getId() {
        return id;
    }

    @QueryParam("id")
    public void setId(final Long id) {
        this.id = id;
    }

    @Parameter(name = "recordKey", in = ParameterIn.QUERY, schema =
            @Schema(implementation = String.class))
    public String getRecordKey() {
        return recordKey;
    }

    @QueryParam("recordKey")
    public void setRecordKey(final String recordKey) {
        this.recordKey = recordKey;
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

    @Parameter(name = "recordDate", in = ParameterIn.QUERY, schema =
            @Schema(implementation = OffsetDateTime.class))
    public OffsetDateTime getRecordDate() {
        return recordDate;
    }

    @QueryParam("recordDate")
    public void setRecordDate(final OffsetDateTime recordDate) {
        this.recordDate = recordDate;
    }

    @Parameter(name = "principal", in = ParameterIn.QUERY, schema =
            @Schema(implementation = String.class))
    public String getPrincipal() {
        return principal;
    }

    @QueryParam("principal")
    public void setPrincipal(final String principal) {
        this.principal = principal;
    }
}
