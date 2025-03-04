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
import java.util.List;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;

public class GroupServiceImpl extends AbstractAnyService<GroupTO, GroupCR, GroupUR> implements GroupService {

    protected final GroupDAO groupDAO;

    protected final GroupLogic logic;

    public GroupServiceImpl(
            final SearchCondVisitor searchCondVisitor,
            final GroupDAO groupDAO,
            final GroupLogic logic) {

        super(searchCondVisitor);
        this.groupDAO = groupDAO;
        this.logic = logic;
    }

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return groupDAO;
    }

    @Override
    protected AbstractAnyLogic<GroupTO, GroupCR, GroupUR> getAnyLogic() {
        return logic;
    }

    @Override
    protected GroupUR newUpdateReq(final String key) {
        return new GroupUR.Builder(key).build();
    }

    @Override
    public Response create(final GroupCR createReq) {
        ProvisioningResult<GroupTO> created = logic.create(createReq, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response update(final GroupUR updateReq) {
        return doUpdate(updateReq);
    }

    @Override
    public List<GroupTO> own() {
        return logic.own();
    }

    @Override
    public ExecTO provisionMembers(final String key, final ProvisionAction action) {
        return logic.provisionMembers(key, action);
    }
}
