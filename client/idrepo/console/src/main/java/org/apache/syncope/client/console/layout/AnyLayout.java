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
package org.apache.syncope.client.console.layout;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.console.SyncopeWebApplication;

public class AnyLayout implements Serializable {

    private static final long serialVersionUID = 488645029994410970L;

    private String anyPanelClass;

    @JsonProperty("USER")
    private UserFormLayoutInfo user;

    @JsonProperty("GROUP")
    private GroupFormLayoutInfo group;

    private final Map<String, AnyObjectFormLayoutInfo> anyObjects = new HashMap<>();

    public AnyLayout() {
        this.anyPanelClass = SyncopeWebApplication.get().getDefaultAnyPanelClass();
    }

    public String getAnyPanelClass() {
        return anyPanelClass;
    }

    public void setAnyPanelClass(final String anyPanelClass) {
        this.anyPanelClass = anyPanelClass;
    }

    public UserFormLayoutInfo getUser() {
        return user;
    }

    public void setUser(final UserFormLayoutInfo user) {
        this.user = user;
    }

    public GroupFormLayoutInfo getGroup() {
        return group;
    }

    public void setGroup(final GroupFormLayoutInfo group) {
        this.group = group;
    }

    @JsonAnyGetter
    public Map<String, AnyObjectFormLayoutInfo> getAnyObjects() {
        return anyObjects;
    }

    @JsonAnySetter
    public void setAnyObjects(final String anyType, final AnyObjectFormLayoutInfo layoutInfo) {
        anyObjects.put(anyType, layoutInfo);
    }
}
