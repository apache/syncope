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
package org.syncope.core.workflow.prcsiam;

import com.opensymphony.workflow.WorkflowException;
import java.net.URLEncoder;
import java.util.Map;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.workflow.*;

public class SendEmail extends BaseSendEmail {

    @Override
    protected HtmlEmail getHtmlEmail(Map<String, String> args,
            SyncopeUser syncopeUser)
            throws EmailException, WorkflowException {

        String urlSuffix = ".jsp";
        if (Boolean.valueOf(args.get("parametrize"))) {
            try {
                urlSuffix += "?confirmationLink="
                        + syncopeConfigurationDAO.find(
                        "servicelayer.baseurl").getConfValue()
                        + "?token="
                        + URLEncoder.encode(syncopeUser.getToken(), "UTF-8")
                        + "&userId=" + Utils.getUserId(syncopeUser);
            } catch (Throwable t) {
                log.error("Unexpected exception", t);
            }
        }

        HtmlEmail email = super.getHtmlEmail(args, syncopeUser);
        email.addTo(Utils.getUserId(syncopeUser));
        try {
            email.setHtmlMsg(getEmailBody(syncopeConfigurationDAO.find(
                    "mail.templates.url").getConfValue(),
                    syncopeConfigurationDAO.find(
                    args.get("template.html")).getConfValue(),
                    urlSuffix,
                    urlSuffix.substring(0, urlSuffix.indexOf('=') + 1)));
            email.setTextMsg(getEmailBody(syncopeConfigurationDAO.find(
                    "mail.templates.url").getConfValue(),
                    syncopeConfigurationDAO.find(
                    args.get("template.txt")).getConfValue(),
                    urlSuffix,
                    urlSuffix.substring(0, urlSuffix.indexOf('=') + 1)));
        } catch (MissingConfKeyException e) {
            new WorkflowException(e);
        }

        return email;
    }
}
