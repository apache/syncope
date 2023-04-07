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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;

@JsonPropertyOrder({ "schemas", "scimType", "detail", "status" })
public class SCIMError extends SCIMBean {

    private static final long serialVersionUID = -8836902509266522394L;

    private final List<String> schemas = List.of(Resource.Error.schema());

    private ErrorType scimType;

    @JsonFormat(shape = Shape.STRING)
    private int status;

    private final String detail;

    public SCIMError(final BadRequestException ex) {
        this(ex.getErrorType(), Response.Status.BAD_REQUEST.getStatusCode(), ex.getMessage());
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMError(
            @JsonProperty("scimType") final ErrorType scimType,
            @JsonProperty("status") final int status,
            @JsonProperty("detail") final String detail) {

        this.scimType = scimType;
        this.status = status;
        this.detail = detail;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public ErrorType getScimType() {
        return scimType;
    }

    public String getDetail() {
        return detail;
    }

    public int getStatus() {
        return status;
    }
}
