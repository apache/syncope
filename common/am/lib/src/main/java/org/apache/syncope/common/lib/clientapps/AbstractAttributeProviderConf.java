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
package org.apache.syncope.common.lib.clientapps;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.CaseCanonicalizationMode;

public abstract class AbstractAttributeProviderConf implements UsernameAttributeProviderConf {

    private static final long serialVersionUID = 497016622295991904L;

    protected CaseCanonicalizationMode caseCanonicalizationMode = CaseCanonicalizationMode.NONE;

    public CaseCanonicalizationMode getCaseCanonicalizationMode() {
        return caseCanonicalizationMode;
    }

    public void setCaseCanonicalizationMode(final CaseCanonicalizationMode caseCanonicalizationMode) {
        this.caseCanonicalizationMode = caseCanonicalizationMode;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(caseCanonicalizationMode).
                toHashCode();
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
        AbstractAttributeProviderConf rhs = (AbstractAttributeProviderConf) obj;
        return new EqualsBuilder().
                append(this.caseCanonicalizationMode, rhs.caseCanonicalizationMode).
                isEquals();
    }
}
