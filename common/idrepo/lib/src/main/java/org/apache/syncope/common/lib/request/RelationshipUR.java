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
package org.apache.syncope.common.lib.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;

public class RelationshipUR extends AbstractPatch {

    private static final long serialVersionUID = 1314175521205206511L;

    public static class Builder extends AbstractPatch.Builder<RelationshipUR, Builder> {

        public Builder(final String type) {
            super();
            getInstance().setType(type);
        }

        @Override
        protected RelationshipUR newInstance() {
            return new RelationshipUR();
        }

        public Builder otherEnd(final String type, final String key) {
            getInstance().setOtherEndType(type);
            getInstance().setOtherEndKey(key);
            return this;
        }

        public Builder plainAttr(final Attr plainAttr) {
            getInstance().getPlainAttrs().add(plainAttr);
            return this;
        }

        public Builder plainAttrs(final Attr... plainAttrs) {
            getInstance().getPlainAttrs().addAll(List.of(plainAttrs));
            return this;
        }

        public Builder plainAttrs(final Collection<Attr> plainAttrs) {
            getInstance().getPlainAttrs().addAll(plainAttrs);
            return this;
        }
    }

    private String type;

    private String otherEndType;

    private String otherEndKey;

    private final Set<Attr> plainAttrs = new HashSet<>();

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

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(type).
                append(otherEndType).
                append(otherEndKey).
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
        final RelationshipUR other = (RelationshipUR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(type, other.type).
                append(otherEndType, other.otherEndType).
                append(otherEndKey, other.otherEndKey).
                build();
    }
}
