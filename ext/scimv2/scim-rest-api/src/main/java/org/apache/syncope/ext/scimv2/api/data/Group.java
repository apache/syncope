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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Optional;
import org.apache.syncope.ext.scimv2.api.type.Function;

@JsonPropertyOrder({ "value", "$ref", "display", "type" })
public class Group extends Reference {

    private static final long serialVersionUID = -7184515273837918246L;

    @JsonIgnore
    private final Function type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Group(
            @JsonProperty("value") final String value,
            @JsonProperty("$ref") final String ref,
            @JsonProperty("display") final String display,
            @JsonProperty("type") final Function type) {

        super(value, display, ref);
        this.type = type;
    }

    @JsonProperty
    public String getType() {
        return Optional.ofNullable(type).map(Enum::name).orElse(null);
    }
}
