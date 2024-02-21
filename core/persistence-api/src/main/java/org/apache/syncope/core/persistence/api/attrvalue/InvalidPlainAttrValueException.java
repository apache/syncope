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
package org.apache.syncope.core.persistence.api.attrvalue;

import jakarta.validation.ValidationException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;

public class InvalidPlainAttrValueException extends ValidationException {

    private static final long serialVersionUID = -5023202610580202148L;

    public InvalidPlainAttrValueException(final String errorMessage) {
        super(errorMessage);
    }

    public InvalidPlainAttrValueException(final String errorMessage, final Throwable cause) {
        super(errorMessage, cause);
    }

    public InvalidPlainAttrValueException(final PlainAttrValue value) {
        this("Could not validate " + value.getValue());
    }

    public InvalidPlainAttrValueException(final PlainAttrValue value, final Throwable cause) {
        this("Could not validate " + value.getValue(), cause);
    }
}
