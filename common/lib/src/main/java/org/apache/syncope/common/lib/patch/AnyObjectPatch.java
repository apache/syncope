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
package org.apache.syncope.common.lib.patch;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "anyObjectPatch")
@XmlType
public class AnyObjectPatch extends AnyPatch {

    private static final long serialVersionUID = -1644118942622556097L;

    private StringReplacePatchItem name;

    private final Set<RelationshipPatch> relationships = new HashSet<>();

    private final Set<MembershipPatch> memberships = new HashSet<>();

    public StringReplacePatchItem getName() {
        return name;
    }

    public void setName(final StringReplacePatchItem name) {
        this.name = name;
    }

    @XmlElementWrapper(name = "relationships")
    @XmlElement(name = "relationship")
    @JsonProperty("relationships")
    public Set<RelationshipPatch> getRelationships() {
        return relationships;
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    public Set<MembershipPatch> getMemberships() {
        return memberships;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && name == null && relationships.isEmpty() && memberships.isEmpty();
    }

}
