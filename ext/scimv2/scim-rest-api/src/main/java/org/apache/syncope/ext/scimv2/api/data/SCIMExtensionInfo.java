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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public class SCIMExtensionInfo extends SCIMBean {

    private static final long serialVersionUID = 1310985252565467391L;

    private final Map<String, String> attributes = new HashMap<>();

    @JsonValue
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @JsonAnySetter
    public void add(final String key, final String value) {
        attributes.put(key, value);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return attributes.isEmpty();
    }
}
