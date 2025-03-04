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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ComplianceQuery;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.core.logic.SyncopeLogic;
import org.apache.syncope.core.logic.UserLogic;

public class UserSelfServiceImpl extends AbstractService implements UserSelfService {

    protected final UserLogic logic;

    protected final SyncopeLogic syncopeLogic;

    public UserSelfServiceImpl(final UserLogic logic, final SyncopeLogic syncopeLogic) {
        this.logic = logic;
        this.syncopeLogic = syncopeLogic;
    }

    @Override
    public Response create(final UserCR createReq) {
        if (!syncopeLogic.isSelfRegAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Self registration forbidden by configuration");
            throw sce;
        }

        ProvisioningResult<UserTO> created = logic.selfCreate(createReq, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response read() {
        Triple<String, String, UserTO> self = logic.selfRead();
        return Response.ok().
                header(RESTHeaders.RESOURCE_KEY, self.getRight().getKey()).
                header(RESTHeaders.OWNED_ENTITLEMENTS, self.getLeft()).
                header(RESTHeaders.DELEGATIONS, self.getMiddle()).
                entity(self.getRight()).
                build();
    }

    @Override
    public Response update(final UserUR updateReq) {
        ProvisioningResult<UserTO> updated = logic.selfUpdate(updateReq, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public Response update(final UserTO user) {
        Triple<String, String, UserTO> self = logic.selfRead();
        return update(AnyOperations.diff(user, self.getRight(), false));
    }

    @Override
    public Response status(final StatusR statusR) {
        ProvisioningResult<UserTO> updated = logic.selfStatus(statusR, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public Response delete() {
        ProvisioningResult<UserTO> deleted = logic.selfDelete(isNullPriorityAsync());
        return modificationResponse(deleted);
    }

    @Override
    public Response mustChangePassword(final PasswordPatch password) {
        ProvisioningResult<UserTO> updated = logic.mustChangePassword(password, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public void compliance(final ComplianceQuery query) {
        logic.compliance(query);
    }

    @Override
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (!syncopeLogic.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        logic.requestPasswordReset(username, securityAnswer);
    }

    @Override
    public void confirmPasswordReset(final String token, final String password) {
        if (!syncopeLogic.isPwdResetAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        logic.confirmPasswordReset(token, password);
    }
}
