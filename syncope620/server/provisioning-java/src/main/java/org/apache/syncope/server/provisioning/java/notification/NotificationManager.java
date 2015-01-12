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
package org.apache.syncope.server.provisioning.java.notification;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.server.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.server.persistence.api.dao.ConfDAO;
import org.apache.syncope.server.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.server.persistence.api.dao.NotificationDAO;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.server.persistence.api.dao.TaskDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.entity.Attributable;
import org.apache.syncope.server.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.server.persistence.api.entity.EntityFactory;
import org.apache.syncope.server.persistence.api.entity.Notification;
import org.apache.syncope.server.persistence.api.entity.PlainAttr;
import org.apache.syncope.server.persistence.api.entity.Subject;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.server.persistence.api.entity.task.TaskExec;
import org.apache.syncope.server.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.server.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.server.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.server.provisioning.api.data.UserDataBinder;
import org.apache.syncope.server.misc.ConnObjectUtil;
import org.apache.syncope.server.misc.search.SearchCondConverter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.ToolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create notification tasks that will be executed by NotificationJob.
 *
 * @see NotificationTask
 */
@Component
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

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private RoleDataBinder roleDataBinder;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

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
            final Attributable<?, ?, ?> attributable,
            final Map<String, Object> model) {

        if (attributable != null) {
            connObjectUtil.retrieveVirAttrValues(attributable,
                    attrUtilFactory.getInstance(
                            attributable instanceof User ? AttributableType.USER : AttributableType.ROLE));
        }

        final List<User> recipients = new ArrayList<>();

        if (notification.getRecipients() != null) {
            recipients.addAll(searchDAO.<User>search(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()),
                    SearchCondConverter.convert(notification.getRecipients()),
                    Collections.<OrderByClause>emptyList(), SubjectType.USER));
        }

        if (notification.isSelfAsRecipient() && attributable instanceof User) {
            recipients.add((User) attributable);
        }

        final Set<String> recipientEmails = new HashSet<>();
        final List<UserTO> recipientTOs = new ArrayList<>(recipients.size());
        for (User recipient : recipients) {
            connObjectUtil.retrieveVirAttrValues(recipient, attrUtilFactory.getInstance(AttributableType.USER));

            String email = getRecipientEmail(notification.getRecipientAttrType(),
                    notification.getRecipientAttrName(), recipient);
            if (email == null) {
                LOG.warn("{} cannot be notified: {} not found", recipient, notification.getRecipientAttrName());
            } else {
                recipientEmails.add(email);
                recipientTOs.add(userDataBinder.getUserTO(recipient));
            }
        }

        if (notification.getStaticRecipients() != null) {
            recipientEmails.addAll(notification.getStaticRecipients());
        }

        model.put("recipients", recipientTOs);
        model.put("syncopeConf", this.findAllSyncopeConfs());
        model.put("events", notification.getEvents());

        NotificationTask task = entityFactory.newEntity(NotificationTask.class);
        task.setTraceLevel(notification.getTraceLevel());
        task.getRecipients().addAll(recipientEmails);
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
    public void createTasks(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result condition,
            final Object before,
            final Object output,
            final Object... input) {

        SubjectType subjectType = null;
        Subject<?, ?, ?> subject = null;

        if (before instanceof UserTO) {
            subjectType = SubjectType.USER;
            subject = userDAO.find(((UserTO) before).getKey());
        } else if (output instanceof UserTO) {
            subjectType = SubjectType.USER;
            subject = userDAO.find(((UserTO) output).getKey());
        } else if (before instanceof RoleTO) {
            subjectType = SubjectType.ROLE;
            subject = roleDAO.find(((RoleTO) before).getKey());
        } else if (output instanceof RoleTO) {
            subjectType = SubjectType.ROLE;
            subject = roleDAO.find(((RoleTO) output).getKey());
        }

        LOG.debug("Search notification for [{}]{}", subjectType, subject);

        for (Notification notification : notificationDAO.findAll()) {
            LOG.debug("Notification available user about {}", notification.getUserAbout());
            LOG.debug("Notification available role about {}", notification.getRoleAbout());
            if (notification.isActive()) {

                final Set<String> events = new HashSet<>(notification.getEvents());
                events.retainAll(Collections.<String>singleton(AuditLoggerName.buildEvent(
                        type, category, subcategory, event, condition)));

                if (events.isEmpty()) {
                    LOG.debug("No events found about {}", subject);
                } else if (subjectType == null || subject == null
                        || notification.getUserAbout() == null || notification.getRoleAbout() == null
                        || searchDAO.matches(subject,
                                SearchCondConverter.convert(notification.getUserAbout()), subjectType)
                        || searchDAO.matches(subject,
                                SearchCondConverter.convert(notification.getRoleAbout()), subjectType)) {

                    LOG.debug("Creating notification task for events {} about {}", events, subject);

                    final Map<String, Object> model = new HashMap<>();
                    model.put("type", type);
                    model.put("category", category);
                    model.put("subcategory", subcategory);
                    model.put("event", event);
                    model.put("condition", condition);
                    model.put("before", before);
                    model.put("output", output);
                    model.put("input", input);

                    if (subject instanceof User) {
                        model.put("user", userDataBinder.getUserTO((User) subject));
                    } else if (subject instanceof Role) {
                        model.put("role", roleDataBinder.getRoleTO((Role) subject));
                    }

                    taskDAO.save(getNotificationTask(notification, subject, model));
                }
            } else {
                LOG.debug("Notification {}, userAbout {}, roleAbout {} is deactivated, "
                        + "notification task will not be created", notification.getKey(),
                        notification.getUserAbout(), notification.getRoleAbout());
            }
        }
    }

    private String getRecipientEmail(
            final IntMappingType recipientAttrType, final String recipientAttrName, final User user) {

        String email = null;

        switch (recipientAttrType) {
            case Username:
                email = user.getUsername();
                break;

            case UserSchema:
                UPlainAttr attr = user.getPlainAttr(recipientAttrName);
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
                    email = derAttr.getValue(user.getPlainAttrs());
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
        NotificationTask task = taskDAO.find(execution.getTask().getKey());
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
        Map<String, String> syncopeConfMap = new HashMap<>();
        for (PlainAttr attr : confDAO.get().getPlainAttrs()) {
            syncopeConfMap.put(attr.getSchema().getKey(), attr.getValuesAsStrings().get(0));
        }
        return syncopeConfMap;
    }
}
