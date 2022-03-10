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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;

public class DefaultNotificationJobDelegate implements NotificationJobDelegate {

    protected static final Logger LOG = LoggerFactory.getLogger(NotificationJobDelegate.class);

    protected final TaskDAO taskDAO;

    protected final JavaMailSender mailSender;

    protected final EntityFactory entityFactory;

    protected final AuditManager auditManager;

    protected final NotificationManager notificationManager;

    protected final AtomicReference<String> status = new AtomicReference<>();

    protected boolean interrupt;

    protected boolean interrupted;

    public DefaultNotificationJobDelegate(
            final TaskDAO taskDAO,
            final JavaMailSender mailSender,
            final EntityFactory entityFactory,
            final AuditManager auditManager,
            final NotificationManager notificationManager) {

        this.taskDAO = taskDAO;
        this.mailSender = mailSender;
        this.entityFactory = entityFactory;
        this.auditManager = auditManager;
        this.notificationManager = notificationManager;
    }

    @Override
    public String currentStatus() {
        return status.get();
    }

    @Override
    public void interrupt() {
        interrupt = true;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    @Transactional
    @Override
    public TaskExec executeSingle(final NotificationTask task, final String executor) {
        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setTask(task);
        execution.setStart(OffsetDateTime.now());
        execution.setExecutor(executor);
        boolean retryPossible = true;

        if (StringUtils.isBlank(task.getSubject()) || task.getRecipients().isEmpty()
                || StringUtils.isBlank(task.getHtmlBody()) || StringUtils.isBlank(task.getTextBody())) {

            String message = "Could not fetch all required information for sending e-mails:\n"
                    + task.getRecipients() + '\n'
                    + task.getSender() + '\n'
                    + task.getSubject() + '\n'
                    + task.getHtmlBody() + '\n'
                    + task.getTextBody();
            LOG.error(message);

            execution.setStatus(NotificationJob.Status.NOT_SENT.name());
            retryPossible = false;

            if (task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal()) {
                execution.setMessage(message);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("About to send e-mails:\n"
                        + task.getRecipients() + '\n'
                        + task.getSender() + '\n'
                        + task.getSubject() + '\n'
                        + task.getHtmlBody() + '\n'
                        + task.getTextBody() + '\n');
            }

            status.set("Sending notifications to " + task.getRecipients());

            for (String to : task.getRecipients()) {
                try {
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

                    notificationManager.createTasks(
                            AuthContextUtils.getWho(),
                            AuditElements.EventCategoryType.TASK,
                            "notification",
                            null,
                            "send",
                            AuditElements.Result.SUCCESS,
                            null,
                            null,
                            task,
                            "Successfully sent notification to " + to);
                } catch (Exception e) {
                    LOG.error("Could not send e-mail", e);

                    execution.setStatus(NotificationJob.Status.NOT_SENT.name());
                    if (task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal()) {
                        execution.setMessage(ExceptionUtils2.getFullStackTrace(e));
                    }

                    notificationManager.createTasks(
                            AuthContextUtils.getWho(),
                            AuditElements.EventCategoryType.TASK,
                            "notification",
                            null,
                            "send",
                            AuditElements.Result.FAILURE,
                            null,
                            null,
                            task,
                            "Could not send notification to " + to, e);
                }

                execution.setEnd(OffsetDateTime.now());
            }
        }

        if (hasToBeRegistered(execution)) {
            execution = notificationManager.storeExec(execution);
            if (retryPossible
                    && (NotificationJob.Status.valueOf(execution.getStatus()) == NotificationJob.Status.NOT_SENT)) {

                handleRetries(execution);
            }
        } else {
            notificationManager.setTaskExecuted(execution.getTask().getKey(), true);
        }

        return execution;
    }

    @Transactional
    @Override
    public void execute(final String executor) throws JobExecutionException {
        List<NotificationTask> tasks = taskDAO.<NotificationTask>findToExec(TaskType.NOTIFICATION);

        status.set("Sending out " + tasks.size() + " notifications");

        for (int i = 0; i < tasks.size() && !interrupt; i++) {
            LOG.debug("Found notification task {} to be executed: starting...", tasks.get(i));
            executeSingle(tasks.get(i), executor);
            LOG.debug("Notification task {} executed", tasks.get(i));
        }
        if (interrupt) {
            LOG.debug("Notification job interrupted");
            interrupted = true;
        }
    }

    protected static boolean hasToBeRegistered(final TaskExec execution) {
        NotificationTask task = (NotificationTask) execution.getTask();

        // True if either failed and failures have to be registered, or if ALL
        // has to be registered.
        return (NotificationJob.Status.valueOf(execution.getStatus()) == NotificationJob.Status.NOT_SENT
                && task.getTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || task.getTraceLevel() == TraceLevel.ALL;
    }

    protected void handleRetries(final TaskExec execution) {
        if (notificationManager.getMaxRetries() <= 0) {
            return;
        }

        long failedExecutionsCount = notificationManager.countExecutionsWithStatus(
                execution.getTask().getKey(), NotificationJob.Status.NOT_SENT.name());

        if (failedExecutionsCount <= notificationManager.getMaxRetries()) {
            LOG.debug("Execution of notification task {} will be retried [{}/{}]",
                    execution.getTask(), failedExecutionsCount, notificationManager.getMaxRetries());
            notificationManager.setTaskExecuted(execution.getTask().getKey(), false);

            auditManager.audit(
                    AuthContextUtils.getWho(),
                    AuditElements.EventCategoryType.TASK,
                    "notification",
                    null,
                    "retry",
                    AuditElements.Result.SUCCESS,
                    null,
                    null,
                    execution,
                    "Notification task " + execution.getTask().getKey() + " will be retried");
        } else {
            LOG.error("Maximum number of retries reached for task {} - giving up", execution.getTask());

            auditManager.audit(
                    AuthContextUtils.getWho(),
                    AuditElements.EventCategoryType.TASK,
                    "notification",
                    null,
                    "retry",
                    AuditElements.Result.FAILURE,
                    null,
                    null,
                    execution,
                    "Giving up retries on notification task " + execution.getTask().getKey());
        }
    }
}
