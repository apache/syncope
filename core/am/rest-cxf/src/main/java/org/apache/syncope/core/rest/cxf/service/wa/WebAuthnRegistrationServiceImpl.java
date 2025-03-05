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
import org.apache.syncope.common.lib.wa.WebAuthnAccount;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.core.logic.wa.WebAuthnRegistrationLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class WebAuthnRegistrationServiceImpl extends AbstractService implements WebAuthnRegistrationService {

    protected final WebAuthnRegistrationLogic logic;

    public WebAuthnRegistrationServiceImpl(final WebAuthnRegistrationLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<WebAuthnAccount> list() {
        return logic.list();
    }

    @Override
    public WebAuthnAccount read(final String owner) {
        return logic.read(owner);
    }

    @Override
    public void delete(final String owner) {
        logic.delete(owner);
    }

    @Override
    public void delete(final String owner, final String credentialId) {
        logic.delete(owner, credentialId);
    }

    @Override
    public void create(final String owner, final WebAuthnAccount account) {
        logic.create(owner, account);
    }

    @Override
    public void update(final String owner, final WebAuthnAccount account) {
        logic.update(owner, account);
    }
}
