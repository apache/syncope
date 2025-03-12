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

import java.util.List;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.core.logic.SAML2SPEntityLogic;

public class SAML2SPEntityServiceImpl extends AbstractService implements SAML2SPEntityService {

    protected final SAML2SPEntityLogic logic;

    public SAML2SPEntityServiceImpl(final SAML2SPEntityLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<SAML2SPEntityTO> list() {
        return logic.list();
    }

    @Override
    public SAML2SPEntityTO get(final String key) {
        return logic.read(key);
    }

    @Override
    public void set(final SAML2SPEntityTO entityTO) {
        logic.set(entityTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
