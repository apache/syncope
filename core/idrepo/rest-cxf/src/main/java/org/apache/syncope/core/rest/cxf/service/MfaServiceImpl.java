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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.MfaService;
import org.apache.syncope.core.logic.MfaLogic;

public class MfaServiceImpl extends AbstractService implements MfaService {

    protected final MfaLogic logic;

    public MfaServiceImpl(final MfaLogic logic) {
        this.logic = logic;
    }

    @Override
    public Mfa generate(final String username) {
        return logic.generate(username);
    }

    @Override
    public void enroll(final Mfa mfa) {
        logic.enroll(mfa);
    }

    @Override
    public void dismiss() {
        logic.dismiss();
    }

    @Override
    public void dismiss(final String username) {
        logic.dismiss(username);
    }

    @Override
    public Response enrolled(final String username) {
        return Response.noContent().header(RESTHeaders.VERIFIED, logic.enrolled(username)).build();
    }

    @Override
    public Response check(final MfaCheck mfaCheck) {
        return Response.noContent().header(RESTHeaders.VERIFIED, logic.check(mfaCheck)).build();
    }
}
