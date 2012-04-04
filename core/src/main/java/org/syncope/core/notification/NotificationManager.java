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

import java.util.ArrayList;
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
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.NotificationDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.scheduling.NotificationJob;
import org.syncope.core.util.ConnObjectUtil;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.WorkflowResult;
import org.syncope.types.IntMappingType;

/**
 * Create notification tasks that will be executed by NotificationJob.
 *
 * @see NotificationTask
 */
@Transactional(rollbackFor = {Throwable.class})
public class NotificationManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

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
     * TaskExec DAO.
     */
    @Autowired
    private TaskExecDAO taskExecDAO;

    /**
     * Velocity template engine.
     */
    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private NotificationJob notificationJob;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param user the user this task is about
     * @param emailSchema name of user schema containing e-mail address
     * @return notification task, fully populated
     */
    private NotificationTask getNotificationTask(final Notification notification, final SyncopeUser user) {

        connObjectUtil.retrieveVirAttrValues(user);

        final List<SyncopeUser> recipients = new ArrayList<SyncopeUser>();
        recipients.addAll(
                searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), notification.getRecipients()));

        if (notification.isSelfAsRecipient()) {
            recipients.add(user);
        }

        Set<String> recipientEmails = new HashSet<String>();

        for (SyncopeUser recipient : recipients) {

            connObjectUtil.retrieveVirAttrValues(recipient);

            String email = getRecipientEmail(
                    notification.getRecipientAttrType(), notification.getRecipientAttrName(), recipient);

            if (email == null) {
                LOG.warn("{} cannot be notified: {} not found", recipient, notification.getRecipientAttrName());
            } else {
                recipientEmails.add(email);
            }
        }

        NotificationTask task = new NotificationTask();
        task.setTraceLevel(notification.getTraceLevel());
        task.setRecipients(recipientEmails);
        task.setSender(notification.getSender());
        task.setSubject(notification.getSubject());

        final Map<String, Object> model = new HashMap<String, Object>();
        for (AbstractAttr attr : user.getAttributes()) {
            List<String> values = attr.getValuesAsStrings();
            model.put(attr.getSchema().getName(), values.isEmpty()
                    ? ""
                    : (values.size() == 1
                    ? values.iterator().next()
                    : values));
        }

        String htmlBody;
        String textBody;
        try {
            htmlBody = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "mailTemplates/"
                    + notification.getTemplate() + ".html.vm", model);
            textBody = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "mailTemplates/"
                    + notification.getTemplate() + ".txt.vm", model);
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
     * Create notification tasks for each notification matching the passed workflow result.
     *
     * @param wfResult workflow result
     * @throws NotFoundException if user contained in the workflow result cannot be found
     */
    public void createTasks(final WorkflowResult<Long> wfResult)
            throws NotFoundException {

        SyncopeUser user = userDAO.find(wfResult.getResult());
        if (user == null) {
            throw new NotFoundException("User " + wfResult.getResult());
        }

        for (Notification notification : notificationDAO.findAll()) {
            if (searchDAO.matches(user, notification.getAbout())) {
                Set<String> events = new HashSet<String>(notification.getEvents());
                events.retainAll(wfResult.getPerformedTasks());

                if (!events.isEmpty()) {
                    LOG.debug("Creating notification task for events {} about {}", events, user);
                    taskDAO.save(getNotificationTask(notification, user));
                } else {
                    LOG.debug("No events found about {}", user);
                }
            }
        }
    }

    public TaskExec execute(final NotificationTask task) {
        return notificationJob.executeSingle(task);
    }

    private String getRecipientEmail(
            final IntMappingType recipientAttrType, final String recipientAttrName, final SyncopeUser user) {

        final String email;

        switch (recipientAttrType) {
            case Username:
                email = user.getUsername();
                break;
            case UserSchema:
                UAttr attr = user.getAttribute(recipientAttrName);
                email = attr == null || attr.getValuesAsStrings().isEmpty() ? null : attr.getValuesAsStrings().get(0);
                break;
            case UserVirtualSchema:
                UVirAttr virAttr = user.getVirtualAttribute(recipientAttrName);
                email = virAttr == null || virAttr.getValues().isEmpty() ? null : virAttr.getValues().get(0);
                break;
            case UserDerivedSchema:
                UDerAttr derAttr = user.getDerivedAttribute(recipientAttrName);
                email = derAttr == null ? null : derAttr.getValue(user.getAttributes());
                break;
            default:
                email = null;
        }

        return email;
    }

    /**
     * Execute TaskExec persist within a transaction.
     *
     * @param execution task execution.
     * @return merged task execution.
     */
    public TaskExec storeExecution(TaskExec execution) {
        NotificationTask task = taskDAO.find(execution.getTask().getId());
        task.addExec(execution);
        return taskExecDAO.save(execution);
    }
}
