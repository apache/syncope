/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyother ownership.  The ASF licenses this file
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.BaseBean;

public class RelationshipTO implements BaseBean, AttributableTO {

    private static final long serialVersionUID = 360672942026613929L;

    public enum End {
        LEFT,
        RIGHT;

    }

    public static class Builder {

        private final RelationshipTO instance = new RelationshipTO();

        public Builder(final String type) {
            instance.setType(type);
            instance.setEnd(End.LEFT);
        }

        public Builder(final String type, final End end) {
            instance.setType(type);
            instance.setEnd(end);
        }

        public Builder otherEnd(final String otherEndType, final String otherEndKey) {
            instance.setOtherEndType(otherEndType);
            instance.setOtherEndKey(otherEndKey);
            return this;
        }

        public Builder otherEnd(final String otherEndType, final String otherEndKey, final String otherEndName) {
            instance.setOtherEndType(otherEndType);
            instance.setOtherEndKey(otherEndKey);
            instance.setOtherEndName(otherEndName);
            return this;
        }

        public Builder plainAttr(final Attr plainAttr) {
            instance.getPlainAttrs().add(plainAttr);
            return this;
        }

        public Builder plainAttrs(final Attr... plainAttrs) {
            instance.getPlainAttrs().addAll(List.of(plainAttrs));
            return this;
        }

        public Builder plainAttrs(final Collection<Attr> plainAttrs) {
            instance.getPlainAttrs().addAll(plainAttrs);
            return this;
        }

        public RelationshipTO build() {
            return instance;
        }
    }

    private String type;

    private End end;

    private String otherEndType;

    private String otherEndKey;

    private String otherEndName;

    private final Set<Attr> plainAttrs = new TreeSet<>();

    private final Set<Attr> derAttrs = new TreeSet<>();

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getOtherEndType() {
        return otherEndType;
    }

    public void setOtherEndType(final String otherEndType) {
        this.otherEndType = otherEndType;
    }

    public String getOtherEndKey() {
        return otherEndKey;
    }

    public void setOtherEndKey(final String otherEndKey) {
        this.otherEndKey = otherEndKey;
    }

    public String getOtherEndName() {
        return otherEndName;
    }

    public void setOtherEndName(final String otherEndName) {
        this.otherEndName = otherEndName;
    }

    public End getEnd() {
        return end;
    }

    public void setEnd(final End end) {
        this.end = end;
    }

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
    @Override
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "derAttrs")
    @JacksonXmlProperty(localName = "derAttr")
    @Override
    public Set<Attr> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getDerAttr(final String schema) {
        return derAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(otherEndType).
                append(otherEndKey).
                append(otherEndName).
                append(end).
                append(plainAttrs).
                append(derAttrs).
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
        final RelationshipTO other = (RelationshipTO) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(otherEndType, other.otherEndType).
                append(otherEndKey, other.otherEndKey).
                append(otherEndName, other.otherEndName).
                append(end, other.end).
                append(plainAttrs, other.plainAttrs).
                append(derAttrs, other.derAttrs).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                append(type).
                append(end).
                append(otherEndType).
                append(otherEndKey).
                build();
    }
}
