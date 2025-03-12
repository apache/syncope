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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.notification.RecipientsProvider;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class DefaultNotificationManager implements NotificationManager {

    protected static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final NotificationDAO notificationDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final AnyMatchDAO anyMatchDAO;

    protected final TaskDAO taskDAO;

    protected final DerAttrHandler derAttrHandler;

    protected final VirAttrHandler virAttrHandler;

    protected final UserDataBinder userDataBinder;

    protected final GroupDataBinder groupDataBinder;

    protected final AnyObjectDataBinder anyObjectDataBinder;

    protected final ConfParamOps confParamOps;

    protected final EntityFactory entityFactory;

    protected final IntAttrNameParser intAttrNameParser;

    protected final SearchCondVisitor searchCondVisitor;

    protected Optional<RecipientsProvider> perContextRecipientsProvider = Optional.empty();

    public DefaultNotificationManager(
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final NotificationDAO notificationDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AnyMatchDAO anyMatchDAO,
            final TaskDAO taskDAO,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final UserDataBinder userDataBinder,
            final GroupDataBinder groupDataBinder,
            final AnyObjectDataBinder anyObjectDataBinder,
            final ConfParamOps confParamOps,
            final EntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser,
            final SearchCondVisitor searchCondVisitor) {

        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.notificationDAO = notificationDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anySearchDAO = anySearchDAO;
        this.anyMatchDAO = anyMatchDAO;
        this.taskDAO = taskDAO;
        this.derAttrHandler = derAttrHandler;
        this.virAttrHandler = virAttrHandler;
        this.userDataBinder = userDataBinder;
        this.groupDataBinder = groupDataBinder;
        this.anyObjectDataBinder = anyObjectDataBinder;
        this.confParamOps = confParamOps;
        this.entityFactory = entityFactory;
        this.intAttrNameParser = intAttrNameParser;
        this.searchCondVisitor = searchCondVisitor;
    }

    @Transactional(readOnly = true)
    @Override
    public long getMaxRetries() {
        return confParamOps.get(SyncopeConstants.MASTER_DOMAIN, "notification.maxRetries", 0L, Long.class);
    }

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param any the any object this task is about
     * @param jexlVars JEXL variables
     * @return notification task, fully populated
     */
    protected NotificationTask getNotificationTask(
            final Notification notification,
            final Any any,
            final Map<String, Object> jexlVars) {

        jexlVars.put("syncopeConf", confParamOps.list(SyncopeConstants.MASTER_DOMAIN));
        jexlVars.put("events", notification.getEvents());

        Optional.ofNullable(any).ifPresent(virAttrHandler::getValues);

        List<User> recipients = new ArrayList<>();

        Optional.ofNullable(notification.getRecipientsFIQL()).
                ifPresent(fiql -> recipients.addAll(anySearchDAO.search(
                SearchCondConverter.convert(searchCondVisitor, fiql), List.of(), AnyTypeKind.USER)));

        if (notification.isSelfAsRecipient() && any instanceof final User user) {
            recipients.add(user);
        }

        Set<String> recipientEmails = new HashSet<>();
        List<UserTO> recipientTOs = new ArrayList<>(recipients.size());
        recipients.forEach(recipient -> {
            virAttrHandler.getValues(recipient);

            Optional.ofNullable(getRecipientEmail(notification.getRecipientAttrName(), recipient)).
                    ifPresentOrElse(
                            email -> {
                                recipientEmails.add(email);
                                recipientTOs.add(userDataBinder.getUserTO(recipient, true));
                            },
                            () -> LOG.warn("{} cannot be notified: {} not found",
                                    recipient, notification.getRecipientAttrName()));
        });
        jexlVars.put("recipients", recipientTOs);

        Optional.ofNullable(notification.getStaticRecipients()).ifPresent(recipientEmails::addAll);

        Optional.ofNullable(notification.getRecipientsProvider()).ifPresent(impl -> {
            try {
                RecipientsProvider recipientsProvider = ImplementationManager.build(
                        impl,
                        () -> perContextRecipientsProvider.orElse(null),
                        instance -> perContextRecipientsProvider = Optional.of(instance));

                recipientEmails.addAll(recipientsProvider.provideRecipients(notification, any, jexlVars));
            } catch (Exception e) {
                LOG.error("While building {}", notification.getRecipientsProvider(), e);
            }
        });

        JexlContext ctx = new MapContext(jexlVars);

        NotificationTask task = entityFactory.newEntity(NotificationTask.class);
        task.setNotification(notification);
        Optional.ofNullable(any).ifPresent(a -> {
            task.setEntityKey(a.getKey());
            task.setAnyTypeKind(a.getType().getKind());
        });
        task.setTraceLevel(notification.getTraceLevel());
        task.getRecipients().addAll(recipientEmails);
        task.setSender(notification.getSender());
        task.setSubject(JexlUtils.evaluateTemplate(notification.getSubject(), ctx));

        if (StringUtils.isNotBlank(notification.getTemplate().getTextTemplate())) {
            task.setTextBody(JexlUtils.evaluateTemplate(notification.getTemplate().getTextTemplate(), ctx));
        }
        if (StringUtils.isNotBlank(notification.getTemplate().getHTMLTemplate())) {
            task.setHtmlBody(JexlUtils.evaluateTemplate(notification.getTemplate().getHTMLTemplate(), ctx));
        }

        return task;
    }

    @Override
    public boolean notificationsAvailable(
            final String domain,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op) {

        String successEvent = OpEvent.toString(type, category, subcategory, op, OpEvent.Outcome.SUCCESS);
        String failureEvent = OpEvent.toString(type, category, subcategory, op, OpEvent.Outcome.FAILURE);
        return AuthContextUtils.callAsAdmin(domain, () -> notificationDAO.findAll().stream().
                anyMatch(notification -> notification.isActive()
                && (notification.getEvents().contains(successEvent)
                || notification.getEvents().contains(failureEvent))));
    }

    @Override
    public void createTasks(final AfterHandlingEvent event) {
        AuthContextUtils.runAsAdmin(event.getDomain(), () -> createTasks(
                event.getWho(),
                event.getType(),
                event.getCategory(),
                event.getSubcategory(),
                event.getOp(),
                event.getOutcome(),
                event.getBefore(),
                event.getOutput(),
                event.getInput()));
    }

    @Override
    public List<NotificationTask> createTasks(
            final String who,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final Object before,
            final Object output,
            final Object... input) {

        Optional<? extends Any> any = Optional.empty();

        if (before instanceof UserTO userTO) {
            any = userDAO.findById(userTO.getKey());
        } else if (output instanceof UserTO userTO) {
            any = userDAO.findById(userTO.getKey());
        } else if (output instanceof final Pair pair
                && pair.getRight() instanceof final UserTO userTO) {

            any = userDAO.findById(userTO.getKey());
        } else if (output instanceof final ProvisioningResult provisioningResult1
                && provisioningResult1.getEntity() instanceof UserTO) {

            any = userDAO.findById(provisioningResult1.getEntity().getKey());
        } else if (before instanceof AnyObjectTO anyObjectTO) {
            any = anyObjectDAO.findById(anyObjectTO.getKey());
        } else if (output instanceof AnyObjectTO anyObjectTO) {
            any = anyObjectDAO.findById(anyObjectTO.getKey());
        } else if (output instanceof final ProvisioningResult result
                && result.getEntity() instanceof AnyObjectTO) {

            any = anyObjectDAO.findById(result.getEntity().getKey());
        } else if (before instanceof GroupTO groupTO) {
            any = groupDAO.findById(groupTO.getKey());
        } else if (output instanceof GroupTO groupTO) {
            any = groupDAO.findById(groupTO.getKey());
        } else if (output instanceof final ProvisioningResult provisioningResult
                && provisioningResult.getEntity() instanceof GroupTO) {

            any = groupDAO.findById(provisioningResult.getEntity().getKey());
        }

        AnyType anyType = any.map(Any::getType).orElse(null);
        LOG.debug("Search notification for [{}]{}", anyType, any);

        List<NotificationTask> notifications = new ArrayList<>();
        for (Notification notification : notificationDAO.findAll()) {
            if (LOG.isDebugEnabled()) {
                notification.getAbouts().
                        forEach(a -> LOG.debug("Notification about {} defined: {}", a.getAnyType(), a.get()));
            }

            if (notification.isActive()) {
                String currentEvent = OpEvent.toString(type, category, subcategory, op, outcome);

                if (!notification.getEvents().contains(currentEvent)) {
                    LOG.debug("No events found about {}", any);
                } else if (anyType == null || any.isEmpty()
                        || notification.getAbout(anyType).isEmpty()
                        || anyMatchDAO.matches(any.get(), SearchCondConverter.convert(
                                searchCondVisitor, notification.getAbout(anyType).get().get()))) {

                    LOG.debug("Creating notification task for event {} about {}", currentEvent, any);

                    Map<String, Object> jexlVars = new HashMap<>();
                    jexlVars.put("who", who);
                    jexlVars.put("type", type);
                    jexlVars.put("category", category);
                    jexlVars.put("subcategory", subcategory);
                    jexlVars.put("event", op);
                    jexlVars.put("condition", outcome);
                    jexlVars.put("before", before);
                    jexlVars.put("output", output);
                    jexlVars.put("input", input);

                    any.ifPresent(a -> {
                        switch (a) {
                            case User user ->
                                jexlVars.put("user", userDataBinder.getUserTO(user, true));
                            case Group group ->
                                jexlVars.put("group", groupDataBinder.getGroupTO(group, true));
                            case AnyObject anyObject ->
                                jexlVars.put("anyObject", anyObjectDataBinder.getAnyObjectTO(anyObject, true));
                            default -> {
                            }
                        }
                    });

                    NotificationTask notificationTask = getNotificationTask(notification, any.orElse(null), jexlVars);
                    notificationTask = taskDAO.save(notificationTask);
                    notifications.add(notificationTask);
                }
            } else {
                LOG.debug("Notification {} is not active, task will not be created", notification.getKey());
            }
        }
        return notifications;
    }

    protected String getRecipientEmail(final String recipientAttrName, final User user) {
        String email = null;

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(recipientAttrName, AnyTypeKind.USER);
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified as recipient, ignoring", recipientAttrName, e);
            return null;
        }

        if ("username".equals(intAttrName.getField())) {
            email = user.getUsername();
        } else if (intAttrName.getSchemaType() != null) {
            UMembership membership = Optional.ofNullable(intAttrName.getMembershipOfGroup()).
                    flatMap(groupDAO::findByName).
                    flatMap(group -> user.getMembership(group.getKey())).
                    orElse(null);

            switch (intAttrName.getSchemaType()) {
                case PLAIN -> {
                    Optional<PlainAttr> attr = membership == null
                            ? user.getPlainAttr(recipientAttrName)
                            : user.getPlainAttr(recipientAttrName, membership);
                    email = attr.map(a -> a.getValuesAsStrings().isEmpty()
                            ? null
                            : a.getValuesAsStrings().getFirst()).
                            orElse(null);
                }

                case DERIVED -> {
                    email = derSchemaDAO.findById(recipientAttrName).
                            map(derSchema -> membership == null
                            ? derAttrHandler.getValue(user, derSchema)
                            : derAttrHandler.getValue(user, membership, derSchema)).
                            orElse(null);
                }

                case VIRTUAL -> {
                    email = virSchemaDAO.findById(recipientAttrName).
                            map(virSchema -> {
                                List<String> virAttrValues = membership == null
                                        ? virAttrHandler.getValues(user, virSchema)
                                        : virAttrHandler.getValues(user, membership, virSchema);
                                return virAttrValues.isEmpty() ? null : virAttrValues.getFirst();
                            }).
                            orElse(null);
                }

                default -> {
                }
            }
        }

        return email;
    }

    @Override
    public TaskExec<NotificationTask> storeExec(final TaskExec<NotificationTask> execution) {
        NotificationTask task = taskDAO.findById(TaskType.NOTIFICATION, execution.getTask().getKey()).
                map(NotificationTask.class::cast).
                orElseThrow(() -> new NotFoundException("NotificationTask " + execution.getTask().getKey()));
        task.add(execution);
        task.setExecuted(true);
        taskDAO.save(task);
        return execution;
    }

    @Override
    public void setTaskExecuted(final String taskKey, final boolean executed) {
        NotificationTask task = taskDAO.findById(TaskType.NOTIFICATION, taskKey).
                map(NotificationTask.class::cast).
                orElseThrow(() -> new NotFoundException("NotificationTask " + taskKey));
        task.setExecuted(executed);
        taskDAO.save(task);
    }

    @Override
    public long countExecutionsWithStatus(final String taskKey, final String status) {
        NotificationTask task = taskDAO.findById(TaskType.NOTIFICATION, taskKey).
                map(NotificationTask.class::cast).
                orElseThrow(() -> new NotFoundException("NotificationTask " + taskKey));
        long count = 0;
        for (TaskExec<NotificationTask> taskExec : task.getExecs()) {
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
}
