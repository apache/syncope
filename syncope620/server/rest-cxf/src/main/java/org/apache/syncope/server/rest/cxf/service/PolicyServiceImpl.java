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
package org.apache.syncope.server.rest.cxf.service;

import java.net.URI;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.AccountPolicyTO;
import org.apache.syncope.common.lib.to.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.SyncPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.server.logic.PolicyLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyServiceImpl extends AbstractServiceImpl implements PolicyService {

    @Autowired
    private PolicyLogic logic;

    @Override
    public <T extends AbstractPolicyTO> Response create(final T policyTO) {
        AbstractPolicyTO policy = logic.create(policyTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(policy.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, policy.getKey()).
                build();
    }

    @Override
    public void delete(final Long policyKey) {
        logic.delete(policyKey);
    }

    @Override
    public <T extends AbstractPolicyTO> List<T> list(final PolicyType type) {
        return logic.list(type);
    }

    @Override
    public <T extends AbstractPolicyTO> T read(final Long policyKey) {
        return logic.read(policyKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractPolicyTO> T readGlobal(final PolicyType type) {
        T result = null;

        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                result = (T) logic.getGlobalAccountPolicy();
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                result = (T) logic.getGlobalPasswordPolicy();
                break;

            case SYNC:
            case GLOBAL_SYNC:
                result = (T) logic.getGlobalSyncPolicy();
                break;

            default:
                throw new BadRequestException();
        }

        return result;
    }

    @Override
    public <T extends AbstractPolicyTO> void update(final Long policyKey, final T policyTO) {
        policyTO.setKey(policyKey);

        switch (policyTO.getType()) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                logic.update((AccountPolicyTO) policyTO);
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                logic.update((PasswordPolicyTO) policyTO);
                break;

            case SYNC:
            case GLOBAL_SYNC:
                logic.update((SyncPolicyTO) policyTO);
                break;

            default:
                break;
        }
    }
}
