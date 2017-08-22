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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.AnyTypeKind;

@XmlRootElement(name = "membership")
@XmlType
public class MembershipTO extends RelationshipTO implements AttributableTO {

    private static final long serialVersionUID = 5992828670273935861L;

    public static class Builder {

        private final MembershipTO instance = new MembershipTO();

        public Builder group(final String groupKey) {
            instance.setRightKey(groupKey);
            return this;
        }

        public Builder group(final String groupKey, final String groupName) {
            instance.setRightKey(groupKey);
            instance.setGroupName(groupName);
            return this;
        }

        public MembershipTO build() {
            return instance;
        }
    }

    private String groupName;

    private final Set<AttrTO> plainAttrs = new HashSet<>();

    private final Set<AttrTO> derAttrs = new HashSet<>();

    private final Set<AttrTO> virAttrs = new HashSet<>();

    @Override
    public String getType() {
        return "Membership";
    }

    @Override
    public void setType(final String relationshipType) {
        // ignore
    }

    @Override
    public String getRightType() {
        return AnyTypeKind.GROUP.name();
    }

    @Override
    public void setRightType(final String rightType) {
        // ignore
    }

    @JsonIgnore
    public String getGroupKey() {
        return getRightKey();
    }

    public void setGroupKey(final String groupKey) {
        setRightKey(groupKey);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    @XmlElementWrapper(name = "plainAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrs")
    @Override
    public Set<AttrTO> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "derAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrs")
    @Override
    public Set<AttrTO> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getDerAttr(final String schema) {
        return derAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    @Override
    public Set<AttrTO> getVirAttrs() {
        return virAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getVirAttr(final String schema) {
        return virAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }
}
