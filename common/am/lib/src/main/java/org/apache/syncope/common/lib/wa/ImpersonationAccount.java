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

public class ImpersonationAccount implements BaseBean {

    private static final long serialVersionUID = 2285073386484048953L;

    public static class Builder {

        private final ImpersonationAccount instance = new ImpersonationAccount();

        public ImpersonationAccount.Builder impersonated(final String impersonated) {
            instance.setImpersonated(impersonated);
            return this;
        }

        public ImpersonationAccount build() {
            return instance;
        }
    }

    private String impersonated;

    public String getImpersonated() {
        return impersonated;
    }

    public void setImpersonated(final String impersonated) {
        this.impersonated = impersonated;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(impersonated)
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
        ImpersonationAccount other = (ImpersonationAccount) obj;
        return new EqualsBuilder()
                .append(this.impersonated, other.impersonated)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("impersonated", impersonated)
                .toString();
    }
}
