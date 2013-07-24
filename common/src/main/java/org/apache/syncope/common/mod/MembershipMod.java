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
package org.apache.syncope.common.mod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType
public class MembershipMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 2511869129977331525L;

    private long role;

    public long getRole() {
        return role;
    }

    public void setRole(long role) {
        this.role = role;
    }

    @Override
    public boolean addResourceToBeAdded(String resource) {
        return false;
    }

    @Override
    public boolean addResourceToBeRemoved(String resource) {
        return false;
    }

    @XmlElementWrapper(name = "resourcesToBeAdded")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToBeAdded")
    @Override
    public Set<String> getResourcesToBeAdded() {
        return Collections.emptySet();
    }

    @XmlElementWrapper(name = "resourcesToBeRemoved")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToBeRemoved")
    @Override
    public Set<String> getResourcesToBeRemoved() {
        return Collections.emptySet();
    }

    @Override
    public boolean removeResourceToBeAdded(String resource) {
        return false;
    }

    @Override
    public boolean removeResourceToBeRemoved(String resource) {
        return false;
    }

    @Override
    public void setResourcesToBeAdded(Set<String> resourcesToBeAdded) {
    }

    @Override
    public void setResourcesToBeRemoved(Set<String> resourcesToBeRemoved) {
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && role == 0;
    }
}
