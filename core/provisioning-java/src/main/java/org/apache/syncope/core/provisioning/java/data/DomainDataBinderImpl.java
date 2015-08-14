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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.provisioning.api.data.DomainDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DomainDataBinderImpl implements DomainDataBinder {

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Domain create(final DomainTO domainTO) {
        Domain domain = entityFactory.newEntity(Domain.class);
        update(domain, domainTO);
        return domain;
    }

    @Override
    public void update(final Domain domain, final DomainTO domainTO) {
        if (domain.getKey() == null) {
            domain.setKey(domainTO.getKey());
        }

        if (StringUtils.isBlank(domainTO.getAdminPwd()) || domainTO.getAdminCipherAlgorithm() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Actual password value and / or cipher algorithm");
        }

        domain.setPassword(domainTO.getAdminPwd(), domainTO.getAdminCipherAlgorithm());
    }

    @Override
    public DomainTO getDomainTO(final Domain domain) {
        DomainTO domainTO = new DomainTO();

        domainTO.setKey(domain.getKey());

        domainTO.setAdminCipherAlgorithm(domain.getAdminCipherAlgorithm());
        domainTO.setAdminPwd(domainTO.getAdminPwd());

        return domainTO;
    }

}
