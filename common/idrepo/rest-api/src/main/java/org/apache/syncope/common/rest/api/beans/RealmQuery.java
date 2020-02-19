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
package org.apache.syncope.common.rest.api.beans;

import java.io.Serializable;
import javax.ws.rs.QueryParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RealmQuery implements Serializable {

    private static final long serialVersionUID = -2278419397595186866L;

    public static class Builder {

        private final RealmQuery instance = new RealmQuery();

        public Builder keyword(final String keyword) {
            instance.setKeyword(keyword);
            return this;
        }

        public Builder base(final String base) {
            instance.setBase(base);
            return this;
        }

        public RealmQuery build() {
            return instance;
        }
    }

    private String keyword;

    private String base;

    public String getKeyword() {
        return keyword;
    }

    @QueryParam("keyword")
    public void setKeyword(final String keyword) {
        this.keyword = keyword;
    }

    public String getBase() {
        return base;
    }

    @QueryParam("base")
    public void setBase(final String base) {
        this.base = base;
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
        RealmQuery other = (RealmQuery) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(keyword, other.keyword).
                append(base, other.base).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(keyword).
                append(base).
                build();
    }
}
