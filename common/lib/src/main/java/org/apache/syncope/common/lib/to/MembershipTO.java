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
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "membership")
@XmlType
public class MembershipTO extends AbstractBaseBean implements AttributableTO {

    private static final long serialVersionUID = 5992828670273935861L;

    public static class Builder {

        private final MembershipTO instance = new MembershipTO();

        public Builder group(final String groupKey) {
            instance.setGroupKey(groupKey);
            return this;
        }

        public Builder group(final String groupKey, final String groupName) {
            instance.setGroupKey(groupKey);
            instance.setGroupName(groupName);
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
    public AttrTO getPlainAttr(final String schema) {
        return IterableUtils.find(plainAttrs, new Predicate<AttrTO>() {

            @Override
            public boolean evaluate(final AttrTO object) {
                return object.getSchema().equals(schema);
            }
        });
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
    public AttrTO getDerAttr(final String schema) {
        return IterableUtils.find(derAttrs, new Predicate<AttrTO>() {

            @Override
            public boolean evaluate(final AttrTO object) {
                return object.getSchema().equals(schema);
            }
        });
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
    public AttrTO getVirAttr(final String schema) {
        return IterableUtils.find(virAttrs, new Predicate<AttrTO>() {

            @Override
            public boolean evaluate(final AttrTO object) {
                return object.getSchema().equals(schema);
            }
        });
    }
}
