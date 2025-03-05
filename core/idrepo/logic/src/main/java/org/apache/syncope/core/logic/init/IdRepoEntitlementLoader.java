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
package org.apache.syncope.core.logic.init;

import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;

public class IdRepoEntitlementLoader implements SyncopeCoreLoader {

    protected final EntitlementAccessor entitlementAccessor;

    public IdRepoEntitlementLoader(final EntitlementAccessor entitlementAccessor) {
        this.entitlementAccessor = entitlementAccessor;
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public void load() {
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());
    }

    @Override
    public void load(final String domain) {
        AuthContextUtils.runAsAdmin(domain, entitlementAccessor::addEntitlementsForAnyTypes);
    }
}
