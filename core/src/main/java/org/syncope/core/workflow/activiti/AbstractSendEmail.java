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

import javax.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Abstract class for sending e-mail from workflow using Sprning, providing an 
 * utility method for such purpose.
 *
 * The main difference with Activiti's MailTask is that this class will not
 * throw any exception when not able to send e-mail.
 */
public abstract class AbstractSendEmail extends AbstractActivitiDelegate {

    /**
     * Utility method for sending e-mail using Spring.
     *
     * @param smtpHost SMTP server host
     * @param to recipient
     * @param from sender
     * @param subject Subject
     * @param textBody E-mail body (text)
     * @param htmlBody E-mail body (HTML)
     */
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
