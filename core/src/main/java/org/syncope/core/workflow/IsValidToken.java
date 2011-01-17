/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Condition;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import org.syncope.core.persistence.beans.user.SyncopeUser;

public class IsValidToken extends OSWorkflowComponent
        implements Condition {

    @Override
    public boolean passesCondition(final Map transientVars,
            final Map args,
            final PropertySet propertySet)
            throws WorkflowException {

        SyncopeUser user = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        final String token = (String) transientVars.get(Constants.TOKEN);

        boolean validToken = user.checkToken(token);

        if (validToken) {
            LOG.debug("Remove valid token '{}': ", token);
            user.removeToken();
        }

        return validToken;
    }
}
