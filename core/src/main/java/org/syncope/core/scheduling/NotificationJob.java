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
package org.syncope.core.scheduling;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.syncope.core.persistence.beans.NotificationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.types.TraceLevel;

/**
 * Periodically checks for notification to send.
 *
 * @see NotificationTask
 */
public class NotificationJob implements StatefulJob {

    enum Status {

        SENT,
        NOT_SENT

    }

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            NotificationJob.class);

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Task execution DAO.
     */
    @Autowired
    private TaskExecDAO taskExecDAO;

    /**
     * Configuration DAO.
     */
    @Autowired
    private ConfDAO confDAO;

    private String smtpHost;

    private int smtpPort;

    private String smtpUsername;

    private String smtpPassword;

    private void init() {
        smtpHost = confDAO.find("smtp.host", "").getValue();
        smtpPort = 25;
        try {
            smtpPort = Integer.valueOf(
                    confDAO.find("smtp.port", "25").getValue());
        } catch (NumberFormatException e) {
            LOG.error("Invalid SMTP port, reverting to 25", e);
        }
        smtpUsername = confDAO.find("smtp.username", "").getValue();
        smtpPassword = confDAO.find("smtp.password", "").getValue();

        LOG.debug("SMTP details fetched: {}:{} / {}:[PASSWORD_NOT_SHOWN]",
                new Object[]{smtpHost, smtpPort, smtpUsername});
    }

    public TaskExec executeSingle(final NotificationTask task) {
        init();

        TaskExec execution = new TaskExec();
        execution.setTask(task);
        execution.setStartDate(new Date());

        if (StringUtils.isBlank(smtpHost)
                || StringUtils.isBlank(task.getSender())
                || StringUtils.isBlank(task.getSubject())
                || task.getRecipients().isEmpty()
                || StringUtils.isBlank(task.getHtmlBody())
                || StringUtils.isBlank(task.getTextBody())) {

            String message = "Could not fetch all required information for "
                    + "sending e-mails:\n"
                    + smtpHost + ":" + smtpPort + "\n"
                    + task.getRecipients() + "\n"
                    + task.getSender() + "\n"
                    + task.getSubject() + "\n"
                    + task.getHtmlBody() + "\n"
                    + task.getTextBody();
            LOG.error(message);

            execution.setStatus(Status.NOT_SENT.name());

            if (task.getTraceLevel().ordinal()
                    >= TraceLevel.FAILURES.ordinal()) {

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
                    if (StringUtils.isNotBlank(smtpUsername)) {
                        sender.setUsername(smtpUsername);
                    }
                    if (StringUtils.isNotBlank(smtpPassword)) {
                        sender.setPassword(smtpPassword);
                    }

                    MimeMessage message = sender.createMimeMessage();
                    MimeMessageHelper helper =
                            new MimeMessageHelper(message, true);

                    helper.setTo(to);
                    helper.setFrom(task.getSender());
                    helper.setSubject(task.getSubject());
                    helper.setText(task.getTextBody(), task.getHtmlBody());

                    sender.send(message);

                    execution.setStatus(Status.SENT.name());

                    StringBuilder report = new StringBuilder();
                    switch (task.getTraceLevel()) {
                        case ALL:
                            report.append("FROM: ").
                                    append(task.getSender()).append('\n').
                                    append("TO: ").append(to).append('\n').
                                    append("SUBJECT: ").
                                    append(task.getSubject()).append('\n').
                                    append('\n').
                                    append(task.getTextBody()).append('\n').
                                    append('\n').
                                    append(task.getHtmlBody()).append('\n');
                            break;

                        case SUMMARY:
                            report.append("E-mail sent to ").
                                    append(to).append('\n');
                            break;

                        case FAILURES:
                        case NONE:
                        default:
                    }
                    if (report.length() > 0) {
                        execution.setMessage(report.toString());
                    }
                } catch (Throwable t) {
                    LOG.error("Could not send e-mail", t);

                    execution.setStatus(Status.NOT_SENT.name());
                    StringWriter exceptionWriter = new StringWriter();
                    exceptionWriter.write(t.getMessage() + "\n\n");
                    t.printStackTrace(new PrintWriter(exceptionWriter));

                    if (task.getTraceLevel().ordinal()
                            >= TraceLevel.FAILURES.ordinal()) {

                        execution.setMessage(exceptionWriter.toString());
                    }
                }

                execution.setEndDate(new Date());
            }
        }

        if (hasToBeRegistered(execution)) {
            execution = taskExecDAO.save(execution);
        }

        return execution;
    }

    @Override
    public void execute(final JobExecutionContext context)
            throws JobExecutionException {

        LOG.debug("Waking up...");

        for (NotificationTask task : taskDAO.findWithoutExecs(
                NotificationTask.class)) {

            executeSingle(task);
        }

        LOG.debug("Sleeping again...");
    }

    private boolean hasToBeRegistered(final TaskExec execution) {
        NotificationTask task = (NotificationTask) execution.getTask();

        // True if either failed and failures have to be registered, or if ALL
        // has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.NOT_SENT
                && task.getTraceLevel().ordinal()
                >= TraceLevel.FAILURES.ordinal())
                || task.getTraceLevel() == TraceLevel.ALL;
    }
}
