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
package org.apache.syncope.core.persistence.jpa.entity;

import org.apache.syncope.core.persistence.api.entity.ConfParam;
import org.apache.syncope.core.persistence.api.entity.DomainEntity;
import org.apache.syncope.core.persistence.api.entity.SelfKeymasterEntityFactory;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.persistence.api.entity.NetworkServiceEntity;

public class JPASelfKeymasterEntityFactory implements SelfKeymasterEntityFactory {

    @Override
    public ConfParam newConfParam() {
        return new JPAConfParam();
    }

    @Override
    public NetworkServiceEntity newNetworkService() {
        JPANetworkService service = new JPANetworkService();
        service.setKey(SecureRandomUtils.generateRandomUUID().toString());
        return service;
    }

    @Override
    public DomainEntity newDomainEntity() {
        return new JPADomain();
    }
}
