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
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;

public class GenerateToken extends OSWorkflowComponent
        implements FunctionProvider {

    protected SyncopeConfigurationDAO syncopeConfigurationDAO;

    public GenerateToken() {
        syncopeConfigurationDAO =
                (SyncopeConfigurationDAO) context.getBean(
                "syncopeConfigurationDAOImpl");
    }

    @Override
    @Transactional
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);
        try {
            syncopeUser.generateToken(
                    Integer.parseInt(syncopeConfigurationDAO.find(
                    "token.length").getConfValue()),
                    Integer.parseInt(syncopeConfigurationDAO.find(
                    "token.expireTime").getConfValue()));
        } catch (MissingConfKeyException e) {
            throw new WorkflowException(e);
        }

        transientVars.put(Constants.SYNCOPE_USER, syncopeUser);
    }
}
