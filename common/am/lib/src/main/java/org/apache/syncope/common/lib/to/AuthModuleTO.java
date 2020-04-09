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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.auth.AuthModuleConf;

@XmlRootElement(name = "authModule")
@XmlType
public class AuthModuleTO extends BaseBean implements EntityTO {

    private static final long serialVersionUID = -7490425997956703057L;

    private String key;

    private String name;

    private String description;

    private final List<ItemTO> profileItems = new ArrayList<>();

    private AuthModuleConf conf;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public AuthModuleConf getConf() {
        return conf;
    }

    public void setConf(final AuthModuleConf conf) {
        this.conf = conf;
    }

    @XmlElementWrapper(name = "profileItems")
    @XmlElement(name = "profileItem")
    @JsonProperty("profileItems")
    public List<ItemTO> getProfileItems() {
        return profileItems;
    }

    public boolean add(final ItemTO item) {
        return Optional.ofNullable(item)
                .filter(itemTO -> this.profileItems.contains(itemTO) || this.profileItems.add(itemTO)).isPresent();
    }

    public boolean remove(final ItemTO item) {
        return this.profileItems.remove(item);
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
        AuthModuleTO other = (AuthModuleTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(description, other.description).
                append(profileItems, other.profileItems).
                append(conf, other.conf).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(description).
                append(profileItems).
                append(conf).
                build();
    }

}
