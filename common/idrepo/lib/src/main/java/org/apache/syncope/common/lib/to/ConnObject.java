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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.BaseBean;

public class ConnObject implements BaseBean {

    private static final long serialVersionUID = 5139554911265442497L;

    private String fiql;

    private final Set<Attr> attrs = new TreeSet<>();

    public String getFiql() {
        return fiql;
    }

    public void setFiql(final String fiql) {
        this.fiql = fiql;
    }

    @JacksonXmlElementWrapper(localName = "attrs")
    @JacksonXmlProperty(localName = "attr")
    public Set<Attr> getAttrs() {
        return attrs;
    }

    @JsonIgnore
    public Optional<Attr> getAttr(final String schema) {
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
        final ConnObject other = (ConnObject) obj;
        return new EqualsBuilder().
                append(fiql, other.fiql).
                append(attrs, other.attrs).
                build();
    }
}
