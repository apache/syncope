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
package org.apache.syncope.core.workflow.java;

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WorkflowContext {

    @ConditionalOnMissingBean
    @Bean
    public UserWorkflowAdapter uwfAdapter(
            final UserDataBinder userDataBinder,
            final UserDAO userDAO,
            final EntityFactory entityFactory,
            final ConfParamOps confParamOps) {

        return new DefaultUserWorkflowAdapter(userDataBinder, userDAO, entityFactory, confParamOps);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupWorkflowAdapter gwfAdapter(
            final GroupDataBinder groupDataBinder,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory) {

        return new DefaultGroupWorkflowAdapter(groupDataBinder, groupDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectWorkflowAdapter awfAdapter(
            final AnyObjectDataBinder anyObjectDataBinder,
            final AnyObjectDAO anyObjectDAO,
            final EntityFactory entityFactory) {

        return new DefaultAnyObjectWorkflowAdapter(anyObjectDataBinder, anyObjectDAO, entityFactory);
    }
}
