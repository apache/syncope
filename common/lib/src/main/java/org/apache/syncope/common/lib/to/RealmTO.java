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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "realm")
@XmlType
public class RealmTO extends AbstractBaseBean {

    private static final long serialVersionUID = 516330662956254391L;

    private long key;

    private String name;

    private long parent;

    private String fullPath;

    private Long accountPolicy;

    private Long passwordPolicy;

    private final Set<String> actionsClassNames = new HashSet<>();

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, AnyTO> templates = new HashMap<>();

    public long getKey() {
        return key;
    }

    public void setKey(final long key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(final long parent) {
        this.parent = parent;
    }

    public String getFullPath() {
        return fullPath;
    }

    @PathParam("fullPath")
    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    public Long getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final Long accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public Long getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final Long passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    @XmlElementWrapper(name = "actionsClassNames")
    @XmlElement(name = "actionsClassName")
    @JsonProperty("actionsClassNames")
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
    }

    @JsonProperty
    public Map<String, AnyTO> getTemplates() {
        return templates;
    }

}
