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

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.logic.init.SCIMLoader;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SCIMLogicContext {

    @ConditionalOnMissingBean
    @Bean
    public SCIMLoader scimLoader() {
        return new SCIMLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMConfManager scimConfManager(final ConfParamOps confParamOps, final SchemaLogic schemaLogic) {
        return new SCIMConfManager(confParamOps, schemaLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMDataBinder scimDataBinder(
            final SCIMConfManager confManager,
            final UserLogic userLogic,
            final AuthDataAccessor authDataAccessor) {

        return new SCIMDataBinder(confManager, userLogic, authDataAccessor);
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMLogic scimLogic(final SCIMConfManager confManager) {
        return new SCIMLogic(confManager);
    }
}
