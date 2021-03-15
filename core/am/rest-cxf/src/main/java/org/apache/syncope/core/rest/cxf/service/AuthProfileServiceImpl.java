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
import org.apache.syncope.common.rest.api.service.AuthProfileService;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthProfileServiceImpl extends AbstractServiceImpl implements AuthProfileService {

    @Autowired
    private AuthProfileLogic logic;

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public void deleteByOwner(final String owner) {
        logic.deleteByOwner(owner);
    }

    @Override
    public AuthProfileTO readByOwner(final String owner) {
        return logic.readByOwner(owner);
    }

    @Override
    public AuthProfileTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public List<AuthProfileTO> list() {
        return logic.list();
    }
}
