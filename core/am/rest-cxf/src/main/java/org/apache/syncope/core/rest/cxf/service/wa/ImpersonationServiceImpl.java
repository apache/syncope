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

package org.apache.syncope.core.rest.cxf.service.wa;

import org.apache.syncope.common.lib.wa.ImpersonatedAccount;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.core.logic.wa.ImpersonationLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.util.List;

@Service
public class ImpersonationServiceImpl extends AbstractServiceImpl implements ImpersonationService {

    @Autowired
    private ImpersonationLogic logic;

    @Override
    public List<ImpersonatedAccount> getImpersonatedAccountsFor(final String impersonator) {
        return logic.getImpersonatedAccountsFor(impersonator);
    }

    @Override
    public Response isImpersonationAttemptAuthorizedFor(final String impersonator,
                                                        final String impersonatee,
                                                        final String application) {
        return logic.isImpersonationAttemptAuthorizedFor(impersonator, impersonatee, application)
            ? Response.ok().build()
            : Response.status(Response.Status.FORBIDDEN).build();
    }

    @Override
    public void authorize(final ImpersonatedAccount account) {
        logic.authorize(account);
    }
}
