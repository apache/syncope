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

import java.util.Date;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractAnyService<UserTO, UserPatch> implements UserService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserLogic logic;

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return userDAO;
    }

    @Override
    protected AbstractAnyLogic<UserTO, UserPatch> getAnyLogic() {
        return logic;
    }

    @Override
    protected UserPatch newPatch(final String key) {
        UserPatch patch = new UserPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    public Response create(final UserTO userTO, final boolean storePassword) {
        ProvisioningResult<UserTO> created = logic.create(userTO, storePassword, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response status(final StatusPatch statusPatch) {
        Date etagDate = findLastChange(statusPatch.getKey());
        checkETag(String.valueOf(etagDate.getTime()));

        ProvisioningResult<UserTO> updated = logic.status(statusPatch, isNullPriorityAsync());
        return modificationResponse(updated);
    }
}
