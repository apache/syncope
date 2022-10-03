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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.syncope.common.lib.BaseBean;

public class CommandTO implements BaseBean {

    private static final long serialVersionUID = 7711356516501958110L;

    private final String key;

    private final List<String> argNames;

    @JsonCreator
    public CommandTO(@JsonProperty("key") final String key, @JsonProperty("argNames") final List<String> argNames) {
        this.key = key;
        this.argNames = argNames;
    }

    public String getKey() {
        return key;
    }

    public List<String> getArgNames() {
        return argNames;
    }
}
