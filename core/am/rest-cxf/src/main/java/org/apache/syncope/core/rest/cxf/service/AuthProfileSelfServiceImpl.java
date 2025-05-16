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

import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.rest.api.service.AuthProfileSelfService;
import org.apache.syncope.core.logic.AuthProfileLogic;

public class AuthProfileSelfServiceImpl extends AbstractService implements AuthProfileSelfService {

    protected final AuthProfileLogic logic;

    public AuthProfileSelfServiceImpl(final AuthProfileLogic logic) {
        this.logic = logic;
    }

    @Override
    public AuthProfileTO read() {
        return logic.selfRead();
    }

    @Override
    public void update(final AuthProfileTO authProfileTO) {
        logic.selfUpdate(authProfileTO);
    }

    @Override
    public void delete() {
        logic.selfDelete();
    }
}
