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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@XmlRootElement(name = "membership")
@XmlType
public class MembershipTO implements Serializable, AttributableTO {

    private static final long serialVersionUID = 5992828670273935861L;

    public static class Builder {

        private final MembershipTO instance = new MembershipTO();

        public Builder(final String groupKey) {
            instance.setGroupKey(groupKey);
        }

        public Builder groupName(final String groupName) {
            instance.setGroupName(groupName);
            return this;
        }

        public Builder plainAttr(final AttrTO plainAttr) {
            instance.getPlainAttrs().add(plainAttr);
            return this;
        }

        public Builder plainAttrs(final AttrTO... plainAttrs) {
            instance.getPlainAttrs().addAll(Arrays.asList(plainAttrs));
            return this;
        }

        public Builder plainAttrs(final Collection<AttrTO> plainAttrs) {
            instance.getPlainAttrs().addAll(plainAttrs);
            return this;
        }

        public Builder virAttr(final AttrTO virAttr) {
            instance.getVirAttrs().add(virAttr);
            return this;
        }

        public Builder virAttrs(final Collection<AttrTO> virAttrs) {
            instance.getVirAttrs().addAll(virAttrs);
            return this;
        }

        public Builder virAttrs(final AttrTO... virAttrs) {
            instance.getVirAttrs().addAll(Arrays.asList(virAttrs));
            return this;
        }

        public MembershipTO build() {
            return instance;
        }
    }

    private String groupKey;

    private String groupName;

    private final Set<AttrTO> plainAttrs = new HashSet<>();

    private final Set<AttrTO> derAttrs = new HashSet<>();

    private final Set<AttrTO> virAttrs = new HashSet<>();

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(final String groupKey) {
        this.groupKey = groupKey;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(groupKey).
                append(groupName).
                append(plainAttrs).
                append(derAttrs).
                append(virAttrs).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MembershipTO other = (MembershipTO) obj;
        return new EqualsBuilder().
                append(groupKey, other.groupKey).
                append(groupName, other.groupName).
                append(plainAttrs, other.plainAttrs).
                append(derAttrs, other.derAttrs).
                append(virAttrs, other.virAttrs).
                build();
    }
}
