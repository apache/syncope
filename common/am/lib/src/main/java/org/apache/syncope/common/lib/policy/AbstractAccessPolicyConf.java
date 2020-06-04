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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.BaseBean;

public abstract class AbstractAccessPolicyConf implements BaseBean, AccessPolicyConf {

    private static final long serialVersionUID = 1153200197344709778L;

    private String name;

    private boolean enabled = true;

    private boolean ssoEnabled = true;

    @JacksonXmlElementWrapper(localName = "requiredAttrs")
    @JacksonXmlProperty(localName = "requiredAttr")
    @JsonProperty("requiredAttrs")
    private final List<Attr> requiredAttrList = new ArrayList<>();

    public AbstractAccessPolicyConf() {
        setName(getClass().getName());
    }

    @Override
    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isSsoEnabled() {
        return this.ssoEnabled;
    }

    public void setSsoEnabled(final boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    @JsonIgnore
    @Override
    public Map<String, Set<String>> getRequiredAttrs() {
        return requiredAttrList.stream().
                collect(Collectors.toUnmodifiableMap(Attr::getSchema, attr -> new HashSet<>(attr.getValues())));
    }

    public void addRequiredAttr(final String key, final Set<String> values) {
        requiredAttrList.removeIf(attr -> attr.getSchema().equals(key));
        requiredAttrList.add(new Attr.Builder(key).values(values).build());
    }
}
