/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.job.notification;

import jakarta.mail.internet.MimeMessage;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class MailNotificationJobDelegate extends AbstractNotificationJobDelegate {

    protected final JavaMailSender mailSender;

    public MailNotificationJobDelegate(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final AuditManager auditManager,
            final NotificationManager notificationManager,
            final ApplicationEventPublisher publisher,
            final JavaMailSender mailSender) {

        super(taskDAO, taskUtilsFactory, auditManager, notificationManager, publisher);
        this.mailSender = mailSender;
    }

    @Override
    protected void notify(
            final String to,
            final NotificationTask task,
            final TaskExec<NotificationTask> execution) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setFrom(task.getSender());
        helper.setSubject(task.getSubject());
        helper.setText(task.getTextBody(), task.getHtmlBody());

        mailSender.send(message);

        execution.setStatus(NotificationJob.Status.SENT.name());

        StringBuilder report = new StringBuilder();
        switch (task.getTraceLevel()) {
            case ALL:
                report.append("FROM: ").append(task.getSender()).append('\n').
                        append("TO: ").append(to).append('\n').
                        append("SUBJECT: ").append(task.getSubject()).append('\n').append('\n').
                        append(task.getTextBody()).append('\n').append('\n').
                        append(task.getHtmlBody()).append('\n');
                break;

            case SUMMARY:
                report.append("E-mail sent to ").append(to).append('\n');
                break;

            case FAILURES:
            case NONE:
            default:
        }
        if (report.length() > 0) {
            execution.setMessage(report.toString());
        }
    }
}
