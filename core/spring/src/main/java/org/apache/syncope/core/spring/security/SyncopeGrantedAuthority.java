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
package org.apache.syncope.core.spring.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.springframework.security.core.GrantedAuthority;

public class SyncopeGrantedAuthority implements GrantedAuthority {

    private static final long serialVersionUID = -5647624636011919735L;

    @JsonProperty
    private final String entitlement;

    private final Set<String> realms = SetUtils.orderedSet(new HashSet<String>());

    @JsonCreator
    public SyncopeGrantedAuthority(@JsonProperty("entitlement") final String entitlement) {
        this.entitlement = entitlement;
    }

    public SyncopeGrantedAuthority(final String entitlement, final String realm) {
        this.entitlement = entitlement;
        this.realms.add(realm);
    }

    public boolean addRealm(final String newRealm) {
        return RealmUtils.normalizingAddTo(realms, newRealm);
    }

    public void addRealms(final Collection<String> newRealms) {
        IterableUtils.forEach(newRealms, new Closure<String>() {

            @Override
            public void execute(final String newRealm) {
                addRealm(newRealm);
            }
        });
    }

    public Set<String> getRealms() {
        return Collections.unmodifiableSet(realms);
    }

    @JsonIgnore
    @Override
    public String getAuthority() {
        return entitlement;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
