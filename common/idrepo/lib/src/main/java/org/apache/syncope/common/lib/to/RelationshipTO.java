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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class RelationshipTO implements BaseBean {

    private static final long serialVersionUID = 360672942026613929L;

    public static class Builder {

        private final RelationshipTO instance = new RelationshipTO();

        public Builder type(final String type) {
            instance.setType(type);
            return this;
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

        public RelationshipTO build() {
            return instance;
        }
    }

    private String type;

    private String otherEndType;

    private String otherEndKey;

    private String otherEndName;

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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(otherEndType).
                append(otherEndKey).
                append(otherEndName).
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
                build();
    }
}
