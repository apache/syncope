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
package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.syncope.core.logic.OIDCC4UILogic;
import org.apache.syncope.core.logic.OIDCC4UIProviderLogic;
import org.apache.syncope.core.rest.cxf.service.OIDCC4UIProviderServiceImpl;
import org.apache.syncope.core.rest.cxf.service.OIDCC4UIServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OIDCC4UIRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public OIDCC4UIService oidcc4UIService(final OIDCC4UILogic oidcc4UILogic) {
        return new OIDCC4UIServiceImpl(oidcc4UILogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCC4UIProviderService oidcc4UIProviderService(final OIDCC4UIProviderLogic oidcc4UIProviderLogic) {
        return new OIDCC4UIProviderServiceImpl(oidcc4UIProviderLogic);
    }
}
