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
package org.apache.syncope.common.lib.wa;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

import javax.ws.rs.PathParam;

public class ImpersonatedAccount implements BaseBean {

    private static final long serialVersionUID = 2285073386484048953L;

    private String id;

    private String owner;

    private String key;

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(key)
            .append(id)
            .append(owner)
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
        ImpersonatedAccount rhs = (ImpersonatedAccount) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.key, rhs.key)
            .append(this.id, rhs.id)
            .append(this.owner, rhs.owner)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("key", key)
            .append("id", id)
            .append("impersonator", owner)
            .toString();
    }

    public static class Builder {

        private final ImpersonatedAccount instance = new ImpersonatedAccount();

        public ImpersonatedAccount.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public ImpersonatedAccount.Builder id(final String id) {
            instance.setId(id);
            return this;
        }

        public ImpersonatedAccount.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public ImpersonatedAccount build() {
            return instance;
        }
    }
}
