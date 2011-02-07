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
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MissingConfKeyException;

public class SendVelocityEmail extends AbstractSendEmail {

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

        String htmlBody =
                VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                "mailTemplates/optin.html.vm",
                model);
        String textBody =
                VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                "mailTemplates/optin.html.vm",
                model);

        String smtpHost;
        try {
            smtpHost = confDAO.find("smtp.host").getConfValue();
        } catch (MissingConfKeyException e) {
            LOG.error("While getting SMTP host", e);
            smtpHost = "";
        }

        super.sendMail(smtpHost,
                user.getAttribute("email").getValuesAsStrings().
                iterator().next(),
                "syncope@googlecode.com",
                "Welcome to Syncope",
                textBody,
                htmlBody);
    }
}
