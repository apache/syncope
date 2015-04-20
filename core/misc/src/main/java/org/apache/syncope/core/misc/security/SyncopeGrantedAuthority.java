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
package org.apache.syncope.core.misc.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.misc.RealmUtils;
import org.springframework.security.core.GrantedAuthority;

public class SyncopeGrantedAuthority implements GrantedAuthority {

    private static final long serialVersionUID = -5647624636011919735L;

    private final Entitlement entitlement;

    private final Set<String> realms = SetUtils.orderedSet(new HashSet<String>());

    public SyncopeGrantedAuthority(final Entitlement entitlement) {
        this.entitlement = entitlement;
    }

    public SyncopeGrantedAuthority(final Entitlement entitlement, final String realm) {
        this.entitlement = entitlement;
        this.realms.add(realm);
    }

    public boolean addRealm(final String newRealm) {
        return RealmUtils.normalizingAddTo(realms, newRealm);
    }

    public void addRealms(final List<String> newRealms) {
        CollectionUtils.forAllDo(newRealms, new Closure<String>() {

            @Override
            public void execute(final String newRealm) {
                addRealm(newRealm);
            }
        });
    }

    public Entitlement getEntitlement() {
        return entitlement;
    }

    public Set<String> getRealms() {
        return Collections.unmodifiableSet(realms);
    }

    @Override
    public String getAuthority() {
        return entitlement.name();
    }

}
