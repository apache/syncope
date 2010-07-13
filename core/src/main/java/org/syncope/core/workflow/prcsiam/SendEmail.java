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

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.*;

public class SendEmail extends BaseSendEmail {

    @Override
    protected HtmlEmail getHtmlEmail(PropertySet ps, String token)
            throws EmailException, WorkflowException {

        String urlPrefix = ps.getString(Constants.BASE_REQUEST_URL)
                + Constants.MAILTEMPLATES_URL;
        String urlSuffix = "";
        try {
            SyncopeConfiguration conf = syncopeConfigurationDAO.find(
                    "servicelayer.baseurl");
            if (conf != null) {
                urlSuffix = ".jsp?confirmationLink="
                        + conf.getConfValue()
                        + "?token=" + URLEncoder.encode(token, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Unexpected exception", e);
        }

        HtmlEmail email = super.getHtmlEmail(ps, token);
        email.addTo(ps.getString("userId"));
        email.setHtmlMsg(getEmailBody(urlPrefix,
                ps.getString(Constants.MAIL_TEMPLATE_HTML),
                urlSuffix,
                urlSuffix.substring(0, urlSuffix.indexOf('=') + 1)));
        email.setTextMsg(getEmailBody(urlPrefix,
                ps.getString(Constants.MAIL_TEMPLATE_TXT),
                urlSuffix,
                urlSuffix.substring(0, urlSuffix.indexOf('=') + 1)));

        return email;
    }

    @Override
    @Transactional
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        if (transientVars.get(Constants.SYNCOPE_USER) != null
                && !ps.exists("userId")) {

            SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                    Constants.SYNCOPE_USER);

            ps.setString("userId", Utils.getUserId(syncopeUser));
        }
        if (transientVars.get(Constants.BASE_REQUEST_URL) != null
                && !ps.exists(Constants.BASE_REQUEST_URL)) {

            ps.setString(Constants.BASE_REQUEST_URL,
                    (String) transientVars.get(Constants.BASE_REQUEST_URL));
        }

        super.execute(transientVars, args, ps);
    }
}
