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
package org.apache.syncope.core.flowable.support;

import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.IdmIdentityServiceImpl;
import org.springframework.context.ConfigurableApplicationContext;

public class SyncopeIdmIdentityService extends IdmIdentityServiceImpl {

    private final ConfigurableApplicationContext ctx;

    public SyncopeIdmIdentityService(
            final IdmEngineConfiguration idmEngineConfiguration,
            final ConfigurableApplicationContext ctx) {

        super(idmEngineConfiguration);
        this.ctx = ctx;
    }

    @Override
    public UserQuery createUserQuery() {
        return ctx.getBeanFactory().createBean(SyncopeUserQueryImpl.class);
    }

    @Override
    public GroupQuery createGroupQuery() {
        return ctx.getBeanFactory().createBean(SyncopeGroupQueryImpl.class);
    }
}
