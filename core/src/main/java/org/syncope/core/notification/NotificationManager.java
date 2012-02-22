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
package org.syncope.core.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.Notification;
import org.syncope.core.persistence.beans.NotificationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.NotificationDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.scheduling.NotificationJob;
import org.syncope.core.workflow.WorkflowResult;

/**
 * Create notification tasks that will be executed by NotificationJob.
 * @see NotificationTask
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class NotificationManager {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationManager.class);

    /**
     * Notification DAO.
     */
    @Autowired
    private NotificationDAO notificationDAO;

    /**
     * Configuration DAO.
     */
    @Autowired
    private ConfDAO confDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * User Search DAO.
     */
    @Autowired
    private UserSearchDAO searchDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Velocity template engine.
     */
    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private NotificationJob notificationJob;

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param user the user this task is about
     * @param emailSchema name of user schema containing e-mail address
     * @return notification task, fully populated
     */
    private NotificationTask getNotificationTask(
            final Notification notification, final SyncopeUser user,
            final String emailSchema) {

        Set<String> recipients = new HashSet<String>();
        for (SyncopeUser recipient : searchDAO.search(
                notification.getRecipients())) {

            if (recipient.getAttribute(emailSchema) == null) {
                LOG.error("{} cannot be notified no {} attribute present",
                        recipient, emailSchema);
            } else {
                recipients.add(recipient.getAttribute(emailSchema).
                        getValuesAsStrings().get(0));
            }
        }
        if (notification.isSelfAsRecipient()) {
            if (user.getAttribute(emailSchema) == null) {
                LOG.error("{} cannot be notified no {} attribute present",
                        user, emailSchema);
            } else {
                recipients.add(user.getAttribute(emailSchema).
                        getValuesAsStrings().get(0));
            }
        }

        NotificationTask task = new NotificationTask();
        task.setTraceLevel(notification.getTraceLevel());
        task.setRecipients(recipients);
        task.setSender(notification.getSender());
        task.setSubject(notification.getSubject());

        final Map<String, Object> model = new HashMap<String, Object>();
        for (AbstractAttr attr : user.getAttributes()) {
            List<String> values = attr.getValuesAsStrings();
            model.put(attr.getSchema().getName(),
                    values.isEmpty()
                    ? ""
                    : (values.size() == 1
                    ? values.iterator().next() : values));
        }

        String htmlBody;
        String textBody;
        try {
            htmlBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + notification.getTemplate() + ".html.vm",
                    model);
            textBody =
                    VelocityEngineUtils.mergeTemplateIntoString(velocityEngine,
                    "mailTemplates/" + notification.getTemplate() + ".txt.vm",
                    model);
        } catch (VelocityException e) {
            LOG.error("Could not get mail body", e);

            htmlBody = "";
            textBody = "";
        }
        task.setTextBody(textBody);
        task.setHtmlBody(htmlBody);

        return task;
    }

    /**
     * Create notification tasks for each notification matching the passed
     * workflow result.
     * @param wfResult workflow result
     * @throws NotFoundException if user contained in the workflow result
     * cannot be found
     */
    public void createTasks(final WorkflowResult<Long> wfResult)
            throws NotFoundException {

        SyncopeUser user = userDAO.find(wfResult.getResult());
        if (user == null) {
            throw new NotFoundException("User " + wfResult.getResult());
        }

        final String emailSchema =
                confDAO.find("email.schema", "email").getValue();

        for (Notification notification : notificationDAO.findAll()) {
            if (searchDAO.matches(user, notification.getAbout())) {
                Set<String> events = new HashSet<String>(
                        notification.getEvents());
                events.retainAll(wfResult.getPerformedTasks());

                if (!events.isEmpty()) {
                    LOG.debug(
                            "Creating notification task for events {} about {}",
                            events, user);

                    taskDAO.save(getNotificationTask(notification, user,
                            emailSchema));
                } else {
                    LOG.debug("No events found about {}", user);
                }
            }
        }
    }

    public TaskExec execute(final NotificationTask task) {
        return notificationJob.executeSingle(task);
    }
}
