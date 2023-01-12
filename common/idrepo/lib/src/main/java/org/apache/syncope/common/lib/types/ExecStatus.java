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
package org.apache.syncope.common.lib.types;

import jakarta.ws.rs.core.Response;

/**
 * Status of some execution.
 */
public enum ExecStatus {

    CREATED(Response.Status.CREATED.getStatusCode()),
    SUCCESS(Response.Status.OK.getStatusCode()),
    FAILURE(Response.Status.BAD_REQUEST.getStatusCode()),
    NOT_ATTEMPTED(Response.Status.PRECONDITION_REQUIRED.getStatusCode());

    protected int httpStatus;

    ExecStatus(final int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public static ExecStatus fromHttpStatus(final int httpStatus) {
        ExecStatus status = null;
        for (ExecStatus value : values()) {
            if (httpStatus == value.getHttpStatus()) {
                status = value;
            }
        }
        if (status == null && httpStatus == Response.Status.NO_CONTENT.getStatusCode()) {
            return ExecStatus.SUCCESS;
        }
        return status;
    }
}
