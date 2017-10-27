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
import com.fasterxml.jackson.annotation.JsonProperty;

public class Display extends SCIMBean {

    private static final long serialVersionUID = 5337055958765320091L;

    private final String value;

    private final String display;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Display(@JsonProperty("value") final String value, @JsonProperty("display") final String display) {
        this.value = value;
        this.display = display;
    }

    public String getValue() {
        return value;
    }

    public String getDisplay() {
        return display;
    }

}
