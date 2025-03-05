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
import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;

public class UserServiceImpl extends AbstractAnyService<UserTO, UserCR, UserUR> implements UserService {

    protected final UserDAO userDAO;

    protected final UserLogic logic;

    public UserServiceImpl(
            final SearchCondVisitor searchCondVisitor,
            final UserDAO userDAO,
            final UserLogic logic) {

        super(searchCondVisitor);
        this.userDAO = userDAO;
        this.logic = logic;
    }

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return userDAO;
    }

    @Override
    protected AbstractAnyLogic<UserTO, UserCR, UserUR> getAnyLogic() {
        return logic;
    }

    @Override
    protected UserUR newUpdateReq(final String key) {
        return new UserUR.Builder(key).build();
    }

    @Override
    public Response create(final UserCR createReq) {
        ProvisioningResult<UserTO> created = logic.create(createReq, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response update(final UserUR updateReq) {
        return doUpdate(updateReq);
    }

    @Override
    public Response status(final StatusR statusR) {
        OffsetDateTime etag = findLastChange(statusR.getKey());
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));

        ProvisioningResult<UserTO> updated = logic.status(statusR, isNullPriorityAsync());
        return modificationResponse(updated);
    }
}
