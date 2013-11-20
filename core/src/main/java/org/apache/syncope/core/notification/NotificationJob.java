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
package org.apache.syncope.core.notification;

import java.util.Date;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.util.ExceptionUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Periodically checks for notification to send.
 *
 * @see NotificationTask
 */
@DisallowConcurrentExecution
public class NotificationJob implements Job {

    enum Status {

        SENT,
        NOT_SENT

    }

    public static String DEFAULT_CRON_EXP = "0 0/5 * * * ?";

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationJob.class);

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private NotificationManager notificationManager;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Configuration DAO.
     */
    @Autowired
    private ConfDAO confDAO;

    private String smtpHost;

    private int smtpPort;

    private String smtpUsername;

    private String smtpPassword;

    private long maxRetries;

    private void init() {
        smtpHost = confDAO.find("smtp.host", "").getValue();
        smtpPort = 25;
        try {
            smtpPort = Integer.valueOf(confDAO.find("smtp.port", "25").getValue());
        } catch (NumberFormatException e) {
            LOG.error("Invalid SMTP port, reverting to 25", e);
        }
        smtpUsername = confDAO.find("smtp.username", "").getValue();
        smtpPassword = confDAO.find("smtp.password", "").getValue();

        LOG.debug("SMTP details fetched: {}:{} / {}:[PASSWORD_NOT_SHOWN]", smtpHost, smtpPort, smtpUsername);

        try {
            maxRetries = Long.valueOf(confDAO.find("notification.maxRetries", "0").getValue());
        } catch (NumberFormatException e) {
            LOG.error("Invalid maximum number of retries, retries disabled", e);
            maxRetries = 0;
        }
    }

    public TaskExec executeSingle(final NotificationTask task) {
        init();

        TaskExec execution = new TaskExec();
        execution.setTask(task);
        execution.setStartDate(new Date());

        boolean retryPossible = true;

        if (StringUtils.isBlank(smtpHost) || StringUtils.isBlank(task.getSender())
                || StringUtils.isBlank(task.getSubject()) || task.getRecipients().isEmpty()
                || StringUtils.isBlank(task.getHtmlBody()) || StringUtils.isBlank(task.getTextBody())) {

            String message = "Could not fetch all required information for sending e-mails:\n"
                    + smtpHost + ":" + smtpPort + "\n"
                    + task.getRecipients() + "\n"
                    + task.getSender() + "\n"
                    + task.getSubject() + "\n"
                    + task.getHtmlBody() + "\n"
                    + task.getTextBody();
            LOG.error(message);

            execution.setStatus(Status.NOT_SENT.name());
            retryPossible = false;

            if (task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal()) {
                execution.setMessage(message);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("About to send e-mails:\n"
                        + smtpHost + ":" + smtpPort + "\n"
                        + task.getRecipients() + "\n"
                        + task.getSender() + "\n"
                        + task.getSubject() + "\n"
                        + task.getHtmlBody() + "\n"
                        + task.getTextBody() + "\n");
            }

            for (String to : task.getRecipients()) {
                try {
                    JavaMailSenderImpl sender = new JavaMailSenderImpl();
                    sender.setHost(smtpHost);
                    sender.setPort(smtpPort);
                    sender.setDefaultEncoding(SyncopeConstants.DEFAULT_ENCODING);
                    if (StringUtils.isNotBlank(smtpUsername)) {
                        sender.setUsername(smtpUsername);
                    }
                    if (StringUtils.isNotBlank(smtpPassword)) {
                        sender.setPassword(smtpPassword);
                    }

                    MimeMessage message = sender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true);
                    helper.setTo(to);
                    helper.setFrom(task.getSender());
                    helper.setSubject(task.getSubject());
                    helper.setText(task.getTextBody(), task.getHtmlBody());

                    sender.send(message);

                    execution.setStatus(Status.SENT.name());

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

                    auditManager.audit(
                            AuditElements.EventCategoryType.TASK,
                            "notification",
                            null,
                            "send",
                            Result.SUCCESS,
                            null,
                            null,
                            task,
                            "Successfully sent notification to " + to);
                } catch (Exception e) {
                    LOG.error("Could not send e-mail", e);

                    execution.setStatus(Status.NOT_SENT.name());
                    if (task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal()) {
                        execution.setMessage(ExceptionUtil.getFullStackTrace(e));
                    }

                    auditManager.audit(
                            AuditElements.EventCategoryType.TASK,
                            "notification",
                            null,
                            "send",
                            Result.FAILURE,
                            null,
                            null,
                            task,
                            "Could not send notification to " + to, e);
                }

                execution.setEndDate(new Date());
            }
        }

        if (hasToBeRegistered(execution)) {
            execution = notificationManager.storeExec(execution);
            if (retryPossible && (Status.valueOf(execution.getStatus()) == Status.NOT_SENT)) {
                handleRetries(execution);
            }
        } else {
            notificationManager.setTaskExecuted(execution.getTask().getId(), true);
        }

        return execution;
    }

    @Override
    public void execute(final JobExecutionContext context)
            throws JobExecutionException {

        LOG.debug("Waking up...");

        for (NotificationTask task : taskDAO.findToExec(NotificationTask.class)) {
            LOG.debug("Found notification task {} to be executed: starting...", task);
            executeSingle(task);
            LOG.debug("Notification task {} executed", task);
        }

        LOG.debug("Sleeping again...");
    }

    private boolean hasToBeRegistered(final TaskExec execution) {
        NotificationTask task = (NotificationTask) execution.getTask();

        // True if either failed and failures have to be registered, or if ALL
        // has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.NOT_SENT
                && task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || task.getTraceLevel() == TraceLevel.ALL;
    }

    private void handleRetries(final TaskExec execution) {
        if (maxRetries <= 0) {
            return;
        }

        long failedExecutionsCount = notificationManager.countExecutionsWithStatus(
                execution.getTask().getId(), Status.NOT_SENT.name());

        if (failedExecutionsCount <= maxRetries) {
            LOG.debug("Execution of notification task {} will be retried [{}/{}]",
                    execution.getTask(), failedExecutionsCount, maxRetries);
            notificationManager.setTaskExecuted(execution.getTask().getId(), false);

            auditManager.audit(
                    AuditElements.EventCategoryType.TASK,
                    "notification",
                    null,
                    "retry",
                    Result.SUCCESS,
                    null,
                    null,
                    execution,
                    "Notification task " + execution.getTask().getId() + " will be retried");
        } else {
            LOG.error("Maximum number of retries reached for task {} - giving up", execution.getTask());

            auditManager.audit(
                    AuditElements.EventCategoryType.TASK,
                    "notification",
                    null,
                    "retry",
                    Result.FAILURE,
                    null,
                    null,
                    execution,
                    "Giving up retries on notification task " + execution.getTask().getId());
        }
    }
}
