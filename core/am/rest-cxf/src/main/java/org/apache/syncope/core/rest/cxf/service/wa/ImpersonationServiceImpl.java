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

import java.util.List;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.core.logic.wa.ImpersonationLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class ImpersonationServiceImpl extends AbstractService implements ImpersonationService {

    protected final ImpersonationLogic logic;

    public ImpersonationServiceImpl(final ImpersonationLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<ImpersonationAccount> read(final String owner) {
        return logic.read(owner);
    }

    @Override
    public void create(final String owner, final ImpersonationAccount account) {
        logic.create(owner, account);
    }

    @Override
    public void delete(final String owner, final String impersonated) {
        logic.delete(owner, impersonated);
    }
}
