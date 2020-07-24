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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.ws.rs.PathParam;

import java.util.List;

public class WAConfigTO implements EntityTO {

    private static final long serialVersionUID = 2185073386484048953L;

    private String key;

    private List<String> values;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> value) {
        this.values = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(key)
            .append(values)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        WAConfigTO rhs = (WAConfigTO) obj;
        return new EqualsBuilder()
            .append(this.key, rhs.key)
            .append(this.values, rhs.values)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("key", key)
            .append("value", values)
            .toString();
    }

    public static class Builder {

        private final WAConfigTO instance = new WAConfigTO();

        public WAConfigTO.Builder value(final List<String> value) {
            instance.setValues(value);
            return this;
        }

        public WAConfigTO.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public WAConfigTO build() {
            return instance;
        }
    }
}
