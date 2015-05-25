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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AnyTO extends ConnObjectTO {

    private static final long serialVersionUID = -754311920679872084L;

    private long key;

    private String type;

    private String realm;

    private String status;

    private final Set<AttrTO> derAttrs = new LinkedHashSet<>();

    private final Set<AttrTO> virAttrs = new LinkedHashSet<>();

    private final Set<String> resources = new HashSet<>();

    private final List<PropagationStatus> propagationStatusTOs = new ArrayList<>();

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final List<Long> dynGroups = new ArrayList<>();

    public long getKey() {
        return key;
    }

    public void setKey(final long key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @XmlElementWrapper(name = "derAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrs")
    public Set<AttrTO> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    public Map<String, AttrTO> getDerAttrMap() {
        Map<String, AttrTO> result = new HashMap<>(derAttrs.size());
        for (AttrTO attributeTO : derAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return Collections.unmodifiableMap(result);
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    public Set<AttrTO> getVirAttrs() {
        return virAttrs;
    }

    @JsonIgnore
    public Map<String, AttrTO> getVirAttrMap() {
        Map<String, AttrTO> result = new HashMap<>(virAttrs.size());
        for (AttrTO attributeTO : virAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return Collections.unmodifiableMap(result);
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public Set<String> getResources() {
        return resources;
    }

    @XmlElementWrapper(name = "propagationStatuses")
    @XmlElement(name = "propagationStatus")
    @JsonProperty("propagationStatuses")
    public List<PropagationStatus> getPropagationStatusTOs() {
        return propagationStatusTOs;
    }

    @XmlElementWrapper(name = "relationships")
    @XmlElement(name = "relationship")
    @JsonProperty("relationships")
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JsonIgnore
    public Map<Long, MembershipTO> getMembershipMap() {
        Map<Long, MembershipTO> result;

        if (getMemberships() == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<>(getMemberships().size());
            for (MembershipTO membership : getMemberships()) {
                result.put(membership.getRightKey(), membership);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    @XmlElementWrapper(name = "dynGroups")
    @XmlElement(name = "role")
    @JsonProperty("dynGroups")
    public List<Long> getDynGroups() {
        return dynGroups;
    }

}
