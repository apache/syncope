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
package org.apache.syncope.ext.scimv2.api;

import org.apache.syncope.ext.scimv2.api.type.ErrorType;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -2588839750716910491L;

    private final ErrorType errorType;

    public BadRequestException(final ErrorType errorType) {
        super();
        this.errorType = errorType;
    }

    public BadRequestException(final ErrorType errorType, final String detail) {
        super(detail);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
