/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow;

import java.util.Map;
import java.util.Map.Entry;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.propagation.PropagationByResource;
import org.syncope.core.rest.controller.UnauthorizedRoleException;
import org.syncope.core.rest.data.UserDataBinder;

@Transactional(rollbackFor = {
    Throwable.class
})
public abstract class AbstractUserWorkflowAdapter
        implements UserWorkflowAdapter {

    @Autowired
    protected UserDataBinder dataBinder;

    @Autowired
    protected UserDAO userDAO;

    @Override
    public WorkflowResult<Entry<Long, Boolean>> create(final UserTO userTO)
            throws UnauthorizedRoleException, WorkflowException {

        return create(userTO, false);
    }

    protected abstract WorkflowResult<Long> doActivate(
            SyncopeUser user, String token)
            throws WorkflowException;

    @Override
    public WorkflowResult<Long> activate(final Long userId, final String token)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        return doActivate(dataBinder.getUserFromId(userId), token);
    }

    protected abstract WorkflowResult<Map.Entry<Long, PropagationByResource>> doUpdate(
            SyncopeUser user, UserMod userMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Map.Entry<Long, PropagationByResource>> update(
            final UserMod userMod)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        return doUpdate(dataBinder.getUserFromId(userMod.getId()), userMod);
    }

    protected abstract WorkflowResult<Long> doSuspend(SyncopeUser user)
            throws WorkflowException;

    @Override
    public WorkflowResult<Long> suspend(final Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        return doSuspend(dataBinder.getUserFromId(userId));
    }

    @Override
    public WorkflowResult<Long> suspend(final SyncopeUser user)
            throws UnauthorizedRoleException, WorkflowException {

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    protected abstract WorkflowResult<Long> doReactivate(SyncopeUser user)
            throws WorkflowException;

    @Override
    public WorkflowResult<Long> reactivate(final Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        final SyncopeUser user = dataBinder.getUserFromId(userId);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doDelete(SyncopeUser user)
            throws WorkflowException;

    @Override
    public void delete(final Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        doDelete(dataBinder.getUserFromId(userId));
    }
}
