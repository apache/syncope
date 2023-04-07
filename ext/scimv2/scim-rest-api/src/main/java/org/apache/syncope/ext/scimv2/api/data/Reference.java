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

import com.fasterxml.jackson.annotation.JsonProperty;

abstract class Reference extends Value {

    private static final long serialVersionUID = -6190164044699376089L;

    private final String display;

    @JsonProperty("$ref")
    private final String ref;

    Reference(final String value, final String display, final String ref) {
        super(value);
        this.display = display;
        this.ref = ref;
    }

    public String getDisplay() {
        return display;
    }

    public String getRef() {
        return ref;
    }
}
