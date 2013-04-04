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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.CorrelationRuleClassTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.PolicyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyServiceImpl implements PolicyService, ContextAware {

    @Autowired
    private PolicyController policyController;

    private UriInfo uriInfo;

    @Override
    public <T extends PolicyTO> Response create(final PolicyType type, final T policyTO) {
        PolicyTO policy = policyController.createInternal(policyTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(policy.getId() + "").build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, policy.getId()).build();
    }

    @Override
    public void delete(final PolicyType type, final Long policyId) {
        policyController.delete(policyId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PolicyTO> List<T> list(final PolicyType type) {
        return (List<T>) policyController.list(type.toString());
    }

    @Override
    public <T extends PolicyTO> T read(final PolicyType type, final Long policyId) {
        return policyController.read(policyId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T readGlobal(final PolicyType type) {
        T result = null;

        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                result = (T) policyController.getGlobalAccountPolicy();
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                result = (T) policyController.getGlobalPasswordPolicy();
                break;

            case SYNC:
            case GLOBAL_SYNC:
                result = (T) policyController.getGlobalSyncPolicy();
                break;

            default:
                throw new BadRequestException();
        }

        return result;
    }

    @Override
    public <T extends PolicyTO> void update(final PolicyType type, final Long policyId, final T policyTO) {
        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                policyController.update((AccountPolicyTO) policyTO);
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                policyController.update((PasswordPolicyTO) policyTO);
                break;

            case SYNC:
            case GLOBAL_SYNC:
                policyController.update((SyncPolicyTO) policyTO);
                break;

            default:
                break;
        }
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }

    @Override
    public Set<CorrelationRuleClassTO> getSyncCorrelationRuleClasses(final PolicyType type) {
        Set<CorrelationRuleClassTO> result = null;

        switch (type) {
            case SYNC:
            case GLOBAL_SYNC:

                @SuppressWarnings("unchecked")
                final Set<String> classes = (Set<String>) policyController.getSyncCorrelationRuleClasses().getModel().
                        values().iterator().next();
                result = CollectionWrapper.wrapSyncCorrelationRuleClasses(classes);
                break;

            default:
                throw new BadRequestException();
        }

        return result;
    }
}
