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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.wrap.EntitlementTO;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.misc.security.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntitlementLogic extends AbstractTransactionalLogic<EntitlementTO> {

    @Autowired
    private EntitlementDAO entitlementDAO;

    public List<String> getAll() {
        List<Entitlement> entitlements = entitlementDAO.findAll();
        List<String> result = new ArrayList<>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            result.add(entitlement.getKey());
        }

        return result;
    }

    public Set<String> getOwn() {
        return AuthContextUtil.getOwnedEntitlementNames();
    }

    @Override
    protected EntitlementTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
