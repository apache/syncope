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
package org.apache.syncope.core.workflow;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.workflow.WorkflowException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.syncope.core.init.WorkflowLoader;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.services.UnauthorizedRoleException;
import org.apache.syncope.to.UserTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = {Throwable.class})
public abstract class AbstractUserWorkflowAdapter implements UserWorkflowAdapter {

    @Autowired
    protected UserDataBinder dataBinder;

    @Autowired
    protected UserDAO userDAO;

    @Override
    public Class<? extends WorkflowLoader> getLoaderClass() {
        return null;
    }

    @Override
    public WorkflowResult<Entry<Long, Boolean>> create(final UserTO userTO)
            throws UnauthorizedRoleException, WorkflowException {

        return create(userTO, false);
    }

    protected abstract WorkflowResult<Long> doActivate(SyncopeUser user, String token) throws WorkflowException;

    @Override
    public WorkflowResult<Long> activate(final Long userId, final String token)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        return doActivate(dataBinder.getUserFromId(userId), token);
    }

    protected abstract WorkflowResult<Map.Entry<Long, Boolean>> doUpdate(SyncopeUser user, UserMod userMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> update(final UserMod userMod)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        return doUpdate(dataBinder.getUserFromId(userMod.getId()), userMod);
    }

    protected abstract WorkflowResult<Long> doSuspend(SyncopeUser user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> suspend(final Long userId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        return suspend(dataBinder.getUserFromId(userId));
    }

    @Override
    public WorkflowResult<Long> suspend(final SyncopeUser user) throws UnauthorizedRoleException, WorkflowException {

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    protected abstract WorkflowResult<Long> doReactivate(SyncopeUser user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> reactivate(final Long userId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        final SyncopeUser user = dataBinder.getUserFromId(userId);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doDelete(SyncopeUser user) throws WorkflowException;

    @Override
    public void delete(final Long userId) throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        doDelete(dataBinder.getUserFromId(userId));
    }
}
