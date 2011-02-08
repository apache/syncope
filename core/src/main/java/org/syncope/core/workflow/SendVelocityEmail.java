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
import com.opensymphony.workflow.WorkflowException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MissingConfKeyException;

/**
 * Send e-mail from workflow using Velocity as template mechanism.
 */
public class SendVelocityEmail extends AbstractSendEmail {

    private static final String KIND = "kind";

    private String getConfValue(final String key) {
        String result;
        try {
            result = confDAO.find(key).getConfValue();
        } catch (MissingConfKeyException e) {
            LOG.error("While getting conf '" + key + "'", e);
            result = "";
        }

        return result;
    }

    @Override
    public void execute(final Map transientVars, final Map args,
            final PropertySet propertySet)
            throws WorkflowException {

        final SyncopeUser user =
                (SyncopeUser) transientVars.get(Constants.SYNCOPE_USER);

        VelocityEngine velocityEngine =
                (VelocityEngine) context.getBean("velocityEngine");

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

        final String smtpHost = getConfValue("smtp.host");
        final String from = getConfValue(args.get(KIND)
                + ".email.from");
        final String subject = getConfValue(args.get(KIND)
                + ".email.subject");
        final String to = user.getAttribute("email") != null
                ? user.getAttribute("email").getValuesAsStrings().
                iterator().next() : null;

        String htmlBody;
        String txtBody;
        try {
            htmlBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + args.get(KIND) + ".html.vm",
                    model);
            txtBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + args.get(KIND) + ".txt.vm",
                    model);
        } catch (VelocityException e) {
            LOG.error("Could not get mail body", e);

            htmlBody = "";
            txtBody = "";
        }

        if (smtpHost.isEmpty() || from.isEmpty() || subject.isEmpty()
                || to == null || to.isEmpty()
                || htmlBody.isEmpty() || txtBody.isEmpty()) {

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
