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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.util.LoggerEventUtils;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UVirAttr;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.SearchCondConverter;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.ToolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create notification tasks that will be executed by NotificationJob.
 *
 * @see NotificationTask
 */
@Transactional(rollbackFor = { Throwable.class })
public class NotificationManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    public static final String MAIL_TEMPLATES = "mailTemplates/";

    public static final String MAIL_TEMPLATE_HTML_SUFFIX = ".html.vm";

    public static final String MAIL_TEMPLATE_TEXT_SUFFIX = ".txt.vm";

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
     * Role DAO.
     */
    @Autowired
    private RoleDAO roleDAO;

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * Role data binder.
     */
    @Autowired
    private RoleDataBinder roleDataBinder;

    /**
     * User Search DAO.
     */
    @Autowired
    private SubjectSearchDAO searchDAO;

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

    /**
     * Velocity tool manager.
     */
    @Autowired
    private ToolManager velocityToolManager;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Transactional(readOnly = true)
    public long getMaxRetries() {
        return confDAO.find("notification.maxRetries", "0").getValues().get(0).getLongValue();
    }

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param attributable the user this task is about
     * @param model Velocity model
     * @return notification task, fully populated
     */
    private NotificationTask getNotificationTask(
            final Notification notification,
            final AbstractAttributable attributable,
            final Map<String, Object> model) {

        if (attributable != null) {
            connObjectUtil.retrieveVirAttrValues(attributable,
                    AttributableUtil.getInstance(
                            attributable instanceof SyncopeUser ? AttributableType.USER : AttributableType.ROLE));
        }

        final List<SyncopeUser> recipients = new ArrayList<SyncopeUser>();

        if (notification.getRecipients() != null) {
            recipients.addAll(searchDAO.<SyncopeUser>search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                    SearchCondConverter.convert(notification.getRecipients()),
                    Collections.<OrderByClause>emptyList(), SubjectType.USER));
        }

        if (notification.isSelfAsRecipient() && attributable instanceof SyncopeUser) {
            recipients.add((SyncopeUser) attributable);
        }

        final Set<String> recipientEmails = new HashSet<String>();
        final List<UserTO> recipientTOs = new ArrayList<UserTO>(recipients.size());
        for (SyncopeUser recipient : recipients) {
            connObjectUtil.retrieveVirAttrValues(recipient, AttributableUtil.getInstance(AttributableType.USER));

            String email = getRecipientEmail(notification.getRecipientAttrType(),
                    notification.getRecipientAttrName(), recipient);
            if (email == null) {
                LOG.warn("{} cannot be notified: {} not found", recipient, notification.getRecipientAttrName());
            } else {
                recipientEmails.add(email);
                recipientTOs.add(userDataBinder.getUserTO(recipient, true));
            }
        }

        if (notification.getStaticRecipients() != null) {
            recipientEmails.addAll(notification.getStaticRecipients());
        }

        model.put("recipients", recipientTOs);
        model.put("syncopeConf", this.findAllSyncopeConfs());
        model.put("events", notification.getEvents());

        NotificationTask task = new NotificationTask();
        task.setTraceLevel(notification.getTraceLevel());
        task.setRecipients(recipientEmails);
        task.setSender(notification.getSender());
        task.setSubject(notification.getSubject());

        String htmlBody = mergeTemplateIntoString(
                MAIL_TEMPLATES + notification.getTemplate() + MAIL_TEMPLATE_HTML_SUFFIX, model);
        String textBody = mergeTemplateIntoString(
                MAIL_TEMPLATES + notification.getTemplate() + MAIL_TEMPLATE_TEXT_SUFFIX, model);

        task.setHtmlBody(htmlBody);
        task.setTextBody(textBody);

        return task;
    }

    private String mergeTemplateIntoString(final String templateLocation, final Map<String, Object> model) {
        StringWriter result = new StringWriter();
        try {
            Context velocityContext = createVelocityContext(model);
            velocityEngine.mergeTemplate(templateLocation, SyncopeConstants.DEFAULT_ENCODING, velocityContext, result);
        } catch (VelocityException e) {
            LOG.error("Could not get mail body", e);
        } catch (RuntimeException e) {
            // ensure same behaviour as by using Spring VelocityEngineUtils.mergeTemplateIntoString()
            throw e;
        } catch (Exception e) {
            LOG.error("Could not get mail body", e);
        }

        return result.toString();
    }

    /**
     * Create a Velocity Context for the given model, to be passed to the template for merging.
     *
     * @param model Velocity model
     * @return Velocity context
     */
    protected Context createVelocityContext(Map<String, Object> model) {
        Context toolContext = velocityToolManager.createContext();
        return new VelocityContext(model, toolContext);
    }

    /**
     * Create notification tasks for each notification matching the given user id and (some of) tasks performed.
     */
    public List<NotificationTask> createTasks(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result condition,
            final Object before,
            final Object output,
            final Object... input) {

        SubjectType subjectType = null;
        AbstractSubject subject = null;
        List<NotificationTask> notificationList = new ArrayList<NotificationTask>();

        if (before instanceof UserTO) {
            subjectType = SubjectType.USER;
            subject = userDAO.find(((UserTO) before).getId());
        } else if (output instanceof UserTO) {
            subjectType = SubjectType.USER;
            subject = userDAO.find(((UserTO) output).getId());
        } else if (before instanceof RoleTO) {
            subjectType = SubjectType.ROLE;
            subject = roleDAO.find(((RoleTO) before).getId());
        } else if (output instanceof RoleTO) {
            subjectType = SubjectType.ROLE;
            subject = roleDAO.find(((RoleTO) output).getId());
        }

        LOG.debug("Search notification for [{}]{}", subjectType, subject);

        for (Notification notification : notificationDAO.findAll()) {
            LOG.debug("Notification available user about {}", notification.getUserAbout());
            LOG.debug("Notification available role about {}", notification.getRoleAbout());
            if (notification.isActive()) {

                final Set<String> events = new HashSet<String>(notification.getEvents());
                events.retainAll(Collections.<String>singleton(LoggerEventUtils.buildEvent(
                        type, category, subcategory, event, condition)));

                if (events.isEmpty()) {
                    LOG.debug("No events found about {}", subject);
                } else if (subjectType == null || subject == null
                        || (subjectType == SubjectType.USER && (notification.getUserAbout() == null
                        || searchDAO.matches(subject,
                                SearchCondConverter.convert(notification.getUserAbout()), subjectType)))
                        || subjectType == SubjectType.ROLE && (notification.getRoleAbout() == null
                        || searchDAO.matches(subject,
                                SearchCondConverter.convert(notification.getRoleAbout()), subjectType))) {

                    LOG.debug("Creating notification task for events {} about {}", events, subject);

                    final Map<String, Object> model = new HashMap<String, Object>();
                    model.put("type", type);
                    model.put("category", category);
                    model.put("subcategory", subcategory);
                    model.put("event", event);
                    model.put("condition", condition);
                    model.put("before", before);
                    model.put("output", output);
                    model.put("input", input);

                    if (subject instanceof SyncopeUser) {
                        model.put("user", userDataBinder.getUserTO((SyncopeUser) subject, true));
                    } else if (subject instanceof SyncopeRole) {
                        model.put("role", roleDataBinder.getRoleTO((SyncopeRole) subject, true));
                    }
                    NotificationTask notificationTask = getNotificationTask(notification, subject, model);
                    notificationTask = taskDAO.save(notificationTask);
                    notificationList.add(notificationTask);
                }
            } else {
                LOG.debug("Notification {}, userAbout {}, roleAbout {} is deactivated, "
                        + "notification task will not be created", notification.getId(),
                        notification.getUserAbout(), notification.getRoleAbout());
            }
        }
        return notificationList;
    }

    private String getRecipientEmail(
            final IntMappingType recipientAttrType, final String recipientAttrName, final SyncopeUser user) {

        String email = null;

        switch (recipientAttrType) {
            case Username:
                email = user.getUsername();
                break;

            case UserSchema:
                UAttr attr = user.getAttr(recipientAttrName);
                if (attr != null && !attr.getValuesAsStrings().isEmpty()) {
                    email = attr.getValuesAsStrings().get(0);
                }
                break;

            case UserVirtualSchema:
                UVirAttr virAttr = user.getVirAttr(recipientAttrName);
                if (virAttr != null && !virAttr.getValues().isEmpty()) {
                    email = virAttr.getValues().get(0);
                }
                break;

            case UserDerivedSchema:
                UDerAttr derAttr = user.getDerAttr(recipientAttrName);
                if (derAttr != null) {
                    email = derAttr.getValue(user.getAttrs());
                }
                break;

            default:
        }

        return email;
    }

    /**
     * Store execution of a NotificationTask.
     *
     * @param execution task execution.
     * @return merged task execution.
     */
    public TaskExec storeExec(final TaskExec execution) {
        NotificationTask task = taskDAO.find(execution.getTask().getId());
        task.addExec(execution);
        task.setExecuted(true);
        taskDAO.save(task);
        // this flush call is needed to generate a value for the execution id
        taskDAO.flush();
        return execution;
    }

    /**
     * Set execution state of NotificationTask with provided id.
     *
     * @param taskId task to be updated
     * @param executed execution state
     */
    public void setTaskExecuted(final Long taskId, final boolean executed) {
        NotificationTask task = taskDAO.find(taskId);
        task.setExecuted(executed);
        taskDAO.save(task);
    }

    /**
     * Count the number of task executions of a given task with a given status.
     *
     * @param taskId task id
     * @param status status
     * @return number of task executions
     */
    public long countExecutionsWithStatus(final Long taskId, final String status) {
        NotificationTask task = taskDAO.find(taskId);
        long count = 0;
        for (TaskExec taskExec : task.getExecs()) {
            if (status == null) {
                if (taskExec.getStatus() == null) {
                    count++;
                }
            } else if (status.equals(taskExec.getStatus())) {
                count++;
            }
        }
        return count;
    }

    protected Map<String, String> findAllSyncopeConfs() {
        Map<String, String> syncopeConfMap = new HashMap<String, String>();
        for (AbstractAttr attr : confDAO.get().getAttrs()) {
            syncopeConfMap.put(attr.getSchema().getName(), attr.getValuesAsStrings().get(0));
        }
        return syncopeConfMap;
    }
}
