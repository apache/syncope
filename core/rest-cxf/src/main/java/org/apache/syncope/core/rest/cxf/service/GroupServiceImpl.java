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
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends AbstractAnyService<GroupTO, GroupPatch> implements GroupService {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private GroupLogic logic;

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return groupDAO;
    }

    @Override
    protected AbstractAnyLogic<GroupTO, GroupPatch> getAnyLogic() {
        return logic;
    }

    @Override
    protected GroupPatch newPatch(final String key) {
        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    public Response create(final GroupTO groupTO) {
        ProvisioningResult<GroupTO> created = logic.create(groupTO, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response update(final GroupTO groupTO) {
        groupTO.setKey(getActualKey(getAnyDAO(), groupTO.getKey()));
        GroupTO before = logic.read(groupTO.getKey());

        checkETag(before.getETagValue());

        ProvisioningResult<GroupTO> updated =
                logic.update(AnyOperations.diff(groupTO, before, false), isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public Response update(final GroupPatch groupPatch) {
        return doUpdate(groupPatch);
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
