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
package org.apache.syncope.common.lib.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SCIMExtensionUserConf implements Serializable {

    private static final long serialVersionUID = -9091596628402547645L;

    private String name;

    private String description;

    private final List<SCIMItem> attributes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<SCIMItem> getAttributes() {
        return attributes;
    }

    public boolean add(final SCIMItem item) {
        return Optional.ofNullable(item).
                filter(itemTO -> attributes.contains(itemTO) || attributes.add(itemTO)).
                isPresent();
    }

    @JsonIgnore
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();
        attributes.forEach(item -> map.put(item.getIntAttrName(), item.getExtAttrName()));
        return Collections.unmodifiableMap(map);
    }
}
