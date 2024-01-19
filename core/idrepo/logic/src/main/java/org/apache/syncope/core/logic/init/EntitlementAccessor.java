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
package org.apache.syncope.core.logic.init;

import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional} access to any type data for entitlement init.
 *
 * @see IdRepoEntitlementLoader
 */
public class EntitlementAccessor {

    protected final AnyTypeDAO anyTypeDAO;

    public EntitlementAccessor(final AnyTypeDAO anyTypeDAO) {
        this.anyTypeDAO = anyTypeDAO;
    }

    @Transactional(readOnly = true)
    public void addEntitlementsForAnyTypes() {
        anyTypeDAO.findAll().stream().
                filter(anyType -> anyType != anyTypeDAO.getUser() && anyType != anyTypeDAO.getGroup()).
                forEach(anyType -> EntitlementsHolder.getInstance().addFor(anyType.getKey()));
    }
}
