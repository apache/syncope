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
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

@XmlRootElement(name = "anyObject")
@XmlType
public class AnyObjectTO extends AnyTO implements GroupableRelatableTO {

    private static final long serialVersionUID = 8841697496476959639L;

    private String name;

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final List<MembershipTO> dynMemberships = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @JsonIgnore
    @Override
    public RelationshipTO getRelationship(final String type, final String rightKey) {
        return IterableUtils.find(relationships, new Predicate<RelationshipTO>() {

            @Override
            public boolean evaluate(final RelationshipTO object) {
                return type.equals(object.getType()) && rightKey.equals(object.getRightKey());
            }
        });
    }

    @XmlElementWrapper(name = "relationships")
    @XmlElement(name = "relationship")
    @JsonProperty("relationships")
    @Override
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @JsonIgnore
    @Override
    public MembershipTO getMembership(final String groupKey) {
        return IterableUtils.find(memberships, new Predicate<MembershipTO>() {

            @Override
            public boolean evaluate(final MembershipTO object) {
                return groupKey.equals(object.getGroupKey());
            }
        });
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    @Override
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @XmlElementWrapper(name = "dynMemberships")
    @XmlElement(name = "dynMembership")
    @JsonProperty("dynMemberships")
    @Override
    public List<MembershipTO> getDynMemberships() {
        return dynMemberships;
    }
}
