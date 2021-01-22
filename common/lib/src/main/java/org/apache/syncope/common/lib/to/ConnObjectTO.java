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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@XmlRootElement(name = "connObject")
@XmlType
public class ConnObjectTO implements Serializable {

    private static final long serialVersionUID = 5139554911265442497L;

    private String fiql;

    private final Set<AttrTO> attrs = new LinkedHashSet<>();

    public String getFiql() {
        return fiql;
    }

    public void setFiql(final String fiql) {
        this.fiql = fiql;
    }

    @XmlElementWrapper(name = "attrs")
    @XmlElement(name = "attribute")
    @JsonProperty("attrs")
    public Set<AttrTO> getAttrs() {
        return attrs;
    }

    @JsonIgnore
    public Optional<AttrTO> getAttr(final String schema) {
        return attrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(fiql).
                append(attrs).
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
        final ConnObjectTO other = (ConnObjectTO) obj;
        return new EqualsBuilder().
                append(fiql, other.fiql).
                append(attrs, other.attrs).
                build();
    }
}
