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

package org.apache.syncope.core.logic.wa;

import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.ImpersonatedAccount;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImpersonationLogic extends AbstractAuthProfileLogic {

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public List<ImpersonatedAccount> getImpersonatedAccountsFor(final String impersonator) {
        AuthProfile profile = authProfileDAO.find(impersonator);
        if (profile != null) {
            return profile.getImpersonatedAccounts();
        }
        return List.of();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean isImpersonationAttemptAuthorizedFor(final String impersonator,
                                                       final String impersonatee,
                                                       final String application) {
        AuthProfile profile = authProfileDAO.find(impersonator);
        if (profile != null) {
            return profile.getImpersonatedAccounts()
                .stream()
                .anyMatch(acct -> acct.getId().equalsIgnoreCase(impersonatee));
        }
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.IMPERSONATION_AUTHORIZED_ACCOUNT_CREATE + "')")
    public void authorize(final ImpersonatedAccount account) {

    }
}
