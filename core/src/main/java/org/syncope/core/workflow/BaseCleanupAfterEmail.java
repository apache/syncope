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

public class BaseCleanupAfterEmail extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        ps.remove(Constants.MAIL_TO);
        ps.remove(Constants.MAIL_FROM);
        ps.remove(Constants.MAIL_SUBJECT);
        ps.remove(Constants.MAIL_TEMPLATE_HTML);
        ps.remove(Constants.MAIL_TEMPLATE_TXT);
    }
}
