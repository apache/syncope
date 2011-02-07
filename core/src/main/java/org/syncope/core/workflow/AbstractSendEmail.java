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

import com.opensymphony.workflow.FunctionProvider;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.syncope.core.persistence.dao.ConfDAO;

public abstract class AbstractSendEmail extends OSWorkflowComponent
        implements FunctionProvider {

    protected ConfDAO confDAO;

    public AbstractSendEmail() {
        super();

        confDAO = (ConfDAO) context.getBean("confDAOImpl");
    }

    protected void sendMail(final String smtpHost,
            final String to,
            final String from,
            final String subject,
            final String textBody,
            final String htmlBody) {

        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            sender.send(message);
        } catch (Throwable t) {
            LOG.error("Could not send e-mail", t);
        }
    }
}
