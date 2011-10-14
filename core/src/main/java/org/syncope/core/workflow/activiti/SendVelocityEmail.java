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
package org.syncope.core.workflow.activiti;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.ActivitiUserWorkflowAdapter;

/**
 * Send e-mail from workflow using Velocity for rendering e-mail body.
 */
public class SendVelocityEmail extends AbstractSendEmail {

    @Override
    protected void doExecute(final DelegateExecution execution)
            throws Exception {

        final SyncopeUser user = (SyncopeUser) execution.getVariable(
                ActivitiUserWorkflowAdapter.SYNCOPE_USER);

//        final String emailKind = (String) execution.getVariable(
//                ActivitiUserWorkflowAdapter.EMAIL_KIND);
        final String emailKind = "optin";

        final Map<String, Object> model = new HashMap<String, Object>();
        List<String> values;
        for (AbstractAttr attr : user.getAttributes()) {
            values = attr.getValuesAsStrings();
            model.put(attr.getSchema().getName(),
                    values.isEmpty()
                    ? ""
                    : (values.size() == 1
                    ? values.iterator().next() : values));
        }

        final String smtpHost = confDAO.find("smtp.host", "").getValue();
        final String from =
                confDAO.find(emailKind + ".email.from", "").getValue();
        final String subject = confDAO.find(emailKind + ".email.subject", "").
                getValue();
        final String to = user.getAttribute("email") != null
                ? user.getAttribute("email").getValuesAsStrings().
                iterator().next() : null;

        VelocityEngine velocityEngine = CONTEXT.getBean(VelocityEngine.class);

        String htmlBody;
        String txtBody;
        try {
            htmlBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + emailKind + ".html.vm",
                    model);
            txtBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + emailKind + ".txt.vm",
                    model);
        } catch (VelocityException e) {
            LOG.error("Could not get mail body", e);

            htmlBody = "";
            txtBody = "";
        }

        if (StringUtils.isBlank(smtpHost) || StringUtils.isBlank(from)
                || StringUtils.isBlank(subject) || StringUtils.isBlank(to)
                || StringUtils.isBlank(htmlBody)
                || StringUtils.isBlank(txtBody)) {

            LOG.error("Could not fetch all required information for "
                    + "sending the email:\n"
                    + smtpHost + "\n"
                    + to + "\n"
                    + from + "\n"
                    + subject + "\n"
                    + htmlBody + "\n"
                    + txtBody + "\n");
        } else {
            super.sendMail(smtpHost,
                    to,
                    from,
                    subject,
                    txtBody,
                    htmlBody);
        }
    }
}
