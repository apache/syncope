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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.rest.controller.UnauthorizedRoleException;

/**
 * Simple implementation basically not involving any workflow engine.
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class NoOpUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    @Override
    public Map.Entry<Long, Boolean> create(final UserTO userTO)
            throws WorkflowException {

        SyncopeUser user = new SyncopeUser();
        dataBinder.create(user, userTO);
        user.setStatus("created");
        user = userDAO.save(user);

        return new DefaultMapEntry(user.getId(), Boolean.TRUE);
    }

    @Override
    protected Long doActivate(final SyncopeUser user, final String token)
            throws WorkflowException {

        if (!user.checkToken(token)) {
            throw new WorkflowException(
                    new RuntimeException("Wrong token: " + token));
        }

        user.removeToken();
        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Map.Entry<Long, PropagationByResource> doUpdate(
            final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        PropagationByResource propByRes = dataBinder.update(user, userMod);

        SyncopeUser updated = userDAO.save(user);

        return new DefaultMapEntry(updated.getId(), propByRes);
    }

    @Override
    protected Long doSuspend(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("suspended");
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Long doReactivate(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        userDAO.delete(user);
    }

    @Override
    public Long execute(final UserTO userTO, final String actionId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        return new WorkflowDefinitionTO();
    }

    @Override
    public void updateDefinition(WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public WorkflowFormTO getForm(String workflowId)
            throws NotFoundException, WorkflowException {

        return null;
    }

    @Override
    public WorkflowFormTO claimForm(String taskId, String userName)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }

    @Override
    public Long submitForm(WorkflowFormTO form, String userName)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(
                new UnsupportedOperationException("Not supported."));
    }
}
