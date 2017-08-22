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
package org.apache.syncope.core.provisioning.java.notification;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationRecipientsProvider;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class NotificationManagerImpl implements NotificationManager {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

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
     * AnyObject DAO.
     */
    @Autowired
    private AnyObjectDAO anyObjectDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Group DAO.
     */
    @Autowired
    private GroupDAO groupDAO;

    /**
     * Search DAO.
     */
    @Autowired
    private AnySearchDAO searchDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private DerAttrHandler derAttrHander;

    @Autowired
    private VirAttrHandler virAttrHander;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private GroupDataBinder groupDataBinder;

    @Autowired
    private AnyObjectDataBinder anyObjectDataBinder;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Transactional(readOnly = true)
    @Override
    public long getMaxRetries() {
        return confDAO.find("notification.maxRetries", 0L);
    }

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param any the any object this task is about
     * @param jexlVars JEXL variables
     * @return notification task, fully populated
     */
    private NotificationTask getNotificationTask(
            final Notification notification,
            final Any<?> any,
            final Map<String, Object> jexlVars) {

        if (any != null) {
            virAttrHander.getValues(any);
        }

        List<User> recipients = new ArrayList<>();

        if (notification.getRecipientsFIQL() != null) {
            recipients.addAll(searchDAO.<User>search(
                    SearchCondConverter.convert(notification.getRecipientsFIQL()),
                    Collections.<OrderByClause>emptyList(), AnyTypeKind.USER));
        }

        if (notification.isSelfAsRecipient() && any instanceof User) {
            recipients.add((User) any);
        }

        Set<String> recipientEmails = new HashSet<>();
        List<UserTO> recipientTOs = new ArrayList<>(recipients.size());
        recipients.forEach(recipient -> {
            virAttrHander.getValues(recipient);

            String email = getRecipientEmail(notification.getRecipientAttrName(), recipient);
            if (email == null) {
                LOG.warn("{} cannot be notified: {} not found", recipient, notification.getRecipientAttrName());
            } else {
                recipientEmails.add(email);
                recipientTOs.add(userDataBinder.getUserTO(recipient, true));
            }
        });

        if (notification.getStaticRecipients() != null) {
            recipientEmails.addAll(notification.getStaticRecipients());
        }

        if (notification.getRecipientsProviderClassName() != null) {
            try {
                NotificationRecipientsProvider recipientsProvider =
                        (NotificationRecipientsProvider) ApplicationContextProvider.getBeanFactory().
                                createBean(Class.forName(notification.getRecipientsProviderClassName()),
                                        AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
                recipientEmails.addAll(recipientsProvider.provideRecipients(notification));
            } catch (Exception e) {
                LOG.error("Could not fetch recipients from {}", notification.getRecipientsProviderClassName(), e);
            }
        }

        jexlVars.put("recipients", recipientTOs);
        jexlVars.put("syncopeConf", this.findAllSyncopeConfs());
        jexlVars.put("events", notification.getEvents());

        NotificationTask task = entityFactory.newEntity(NotificationTask.class);
        task.setNotification(notification);
        if (any != null) {
            task.setEntityKey(any.getKey());
            task.setAnyTypeKind(any.getType().getKind());
        }
        task.setTraceLevel(notification.getTraceLevel());
        task.getRecipients().addAll(recipientEmails);
        task.setSender(notification.getSender());
        task.setSubject(notification.getSubject());

        if (StringUtils.isNotBlank(notification.getTemplate().getTextTemplate())) {
            task.setTextBody(evaluate(notification.getTemplate().getTextTemplate(), jexlVars));
        }
        if (StringUtils.isNotBlank(notification.getTemplate().getHTMLTemplate())) {
            task.setHtmlBody(evaluate(notification.getTemplate().getHTMLTemplate(), jexlVars));
        }

        return task;
    }

    private String evaluate(final String template, final Map<String, Object> jexlVars) {
        StringWriter writer = new StringWriter();
        JexlUtils.newJxltEngine().
                createTemplate(template).
                evaluate(new MapContext(jexlVars), writer);
        return writer.toString();
    }

    @Override
    public boolean notificationsAvailable(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event) {

        final String successEvent = AuditLoggerName.buildEvent(type, category, subcategory, event, Result.SUCCESS);
        final String failureEvent = AuditLoggerName.buildEvent(type, category, subcategory, event, Result.FAILURE);
        return notificationDAO.findAll().stream().
                anyMatch(notification -> notification.isActive()
                && (notification.getEvents().contains(successEvent)
                || notification.getEvents().contains(failureEvent)));
    }

    @Override
    public void createTasks(final AfterHandlingEvent event) {
        createTasks(
                event.getType(),
                event.getCategory(),
                event.getSubcategory(),
                event.getEvent(),
                event.getCondition(),
                event.getBefore(),
                event.getOutput(),
                event.getInput());
    }

    @Override
    public List<NotificationTask> createTasks(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result condition,
            final Object before,
            final Object output,
            final Object... input) {

        Any<?> any = null;

        if (before instanceof UserTO) {
            any = userDAO.find(((UserTO) before).getKey());
        } else if (output instanceof UserTO) {
            any = userDAO.find(((UserTO) output).getKey());
        } else if (output instanceof Pair
                && ((Pair) output).getRight() instanceof UserTO) {

            any = userDAO.find(((UserTO) ((Pair) output).getRight()).getKey());
        } else if (output instanceof ProvisioningResult
                && ((ProvisioningResult) output).getEntity() instanceof UserTO) {

            any = userDAO.find(((ProvisioningResult) output).getEntity().getKey());
        } else if (before instanceof AnyObjectTO) {
            any = anyObjectDAO.find(((AnyObjectTO) before).getKey());
        } else if (output instanceof AnyObjectTO) {
            any = anyObjectDAO.find(((AnyObjectTO) output).getKey());
        } else if (output instanceof ProvisioningResult
                && ((ProvisioningResult) output).getEntity() instanceof AnyObjectTO) {

            any = anyObjectDAO.find(((ProvisioningResult) output).getEntity().getKey());
        } else if (before instanceof GroupTO) {
            any = groupDAO.find(((GroupTO) before).getKey());
        } else if (output instanceof GroupTO) {
            any = groupDAO.find(((GroupTO) output).getKey());
        } else if (output instanceof ProvisioningResult
                && ((ProvisioningResult) output).getEntity() instanceof GroupTO) {

            any = groupDAO.find(((ProvisioningResult) output).getEntity().getKey());
        }

        AnyType anyType = any == null ? null : any.getType();
        LOG.debug("Search notification for [{}]{}", anyType, any);

        List<NotificationTask> notifications = new ArrayList<>();
        for (Notification notification : notificationDAO.findAll()) {
            if (LOG.isDebugEnabled()) {
                notification.getAbouts().forEach(about -> {
                    LOG.debug("Notification about {} defined: {}", about.getAnyType(), about.get());
                });
            }

            if (notification.isActive()) {
                String currentEvent = AuditLoggerName.buildEvent(type, category, subcategory, event, condition);
                if (!notification.getEvents().contains(currentEvent)) {
                    LOG.debug("No events found about {}", any);
                } else if (anyType == null || any == null
                        || !notification.getAbout(anyType).isPresent()
                        || searchDAO.matches(
                                any, SearchCondConverter.convert(notification.getAbout(anyType).get().get()))) {

                    LOG.debug("Creating notification task for event {} about {}", currentEvent, any);

                    final Map<String, Object> model = new HashMap<>();
                    model.put("type", type);
                    model.put("category", category);
                    model.put("subcategory", subcategory);
                    model.put("event", event);
                    model.put("condition", condition);
                    model.put("before", before);
                    model.put("output", output);
                    model.put("input", input);

                    if (any instanceof User) {
                        model.put("user", userDataBinder.getUserTO((User) any, true));
                    } else if (any instanceof Group) {
                        model.put("group", groupDataBinder.getGroupTO((Group) any, true));
                    } else if (any instanceof AnyObject) {
                        model.put("group", anyObjectDataBinder.getAnyObjectTO((AnyObject) any, true));
                    }

                    NotificationTask notificationTask = getNotificationTask(notification, any, model);
                    notificationTask = taskDAO.save(notificationTask);
                    notifications.add(notificationTask);
                }
            } else {
                LOG.debug("Notification {} is not active, task will not be created", notification.getKey());
            }
        }
        return notifications;
    }

    private String getRecipientEmail(final String recipientAttrName, final User user) {
        String email = null;

        IntAttrName intAttrName = intAttrNameParser.parse(recipientAttrName, AnyTypeKind.USER);

        if ("username".equals(intAttrName.getField())) {
            email = user.getUsername();
        } else if (intAttrName.getSchemaType() != null) {
            UMembership membership = null;
            if (intAttrName.getMembershipOfGroup() != null) {
                Group group = groupDAO.findByName(intAttrName.getMembershipOfGroup());
                if (group != null) {
                    membership = user.getMembership(group.getKey()).orElse(null);
                }
            }

            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    Optional<? extends UPlainAttr> attr = membership == null
                            ? user.getPlainAttr(recipientAttrName)
                            : user.getPlainAttr(recipientAttrName, membership);
                    if (attr.isPresent()) {
                        email = attr.get().getValuesAsStrings().isEmpty()
                                ? null
                                : attr.get().getValuesAsStrings().get(0);
                    }
                    break;

                case DERIVED:
                    DerSchema schema = derSchemaDAO.find(recipientAttrName);
                    if (schema == null) {
                        LOG.warn("Ignoring non existing {} {}", DerSchema.class.getSimpleName(), recipientAttrName);
                    } else {
                        email = membership == null
                                ? derAttrHander.getValue(user, schema)
                                : derAttrHander.getValue(user, membership, schema);
                    }
                    break;

                case VIRTUAL:
                    VirSchema virSchema = virSchemaDAO.find(recipientAttrName);
                    if (virSchema == null) {
                        LOG.warn("Ignoring non existing {} {}", VirSchema.class.getSimpleName(), recipientAttrName);
                    } else {
                        List<String> virAttrValues = membership == null
                                ? virAttrHander.getValues(user, virSchema)
                                : virAttrHander.getValues(user, membership, virSchema);
                        email = virAttrValues.isEmpty() ? null : virAttrValues.get(0);
                    }
                    break;

                default:
            }
        }

        return email;
    }

    @Override
    public TaskExec storeExec(final TaskExec execution) {
        NotificationTask task = taskDAO.find(execution.getTask().getKey());
        task.add(execution);
        task.setExecuted(true);
        taskDAO.save(task);
        // this flush call is needed to generate a value for the execution key
        taskDAO.flush();
        return execution;
    }

    @Override
    public void setTaskExecuted(final String taskKey, final boolean executed) {
        NotificationTask task = taskDAO.find(taskKey);
        task.setExecuted(executed);
        taskDAO.save(task);
    }

    @Override
    public long countExecutionsWithStatus(final String taskKey, final String status) {
        NotificationTask task = taskDAO.find(taskKey);
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
        for (PlainAttr<?> attr : confDAO.get().getPlainAttrs()) {
            syncopeConfMap.put(attr.getSchema().getKey(), attr.getValuesAsStrings().get(0));
        }
        return syncopeConfMap;
    }
}
