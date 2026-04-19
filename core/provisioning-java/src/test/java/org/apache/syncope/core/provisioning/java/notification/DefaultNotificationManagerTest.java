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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.search.AnySearchCondVisitor;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.EmptyClassLoader;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.api.jexl.SyncopeJexlFunctions;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultNotificationManagerTest {

    private static final String DELETE_SUCCESS = OpEvent.toString(
            OpEvent.CategoryType.LOGIC, "UserLogic", null, "delete", OpEvent.Outcome.SUCCESS);

    @Mock
    private DerSchemaDAO derSchemaDAO;

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private AnyObjectDAO anyObjectDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private AnySearchDAO anySearchDAO;

    @Mock
    private AnyMatchDAO anyMatchDAO;

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private RelationshipTypeDAO relationshipTypeDAO;

    @Mock
    private DerAttrHandler derAttrHandler;

    @Mock
    private UserDataBinder userDataBinder;

    @Mock
    private GroupDataBinder groupDataBinder;

    @Mock
    private AnyObjectDataBinder anyObjectDataBinder;

    @Mock
    private ConfParamOps confParamOps;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private IntAttrNameParser intAttrNameParser;

    @Mock
    private AnySearchCondVisitor searchCondVisitor;

    private JexlTools jexlTools;

    private DefaultNotificationManager manager;

    @BeforeEach
    void init() {
        JexlEngine jexlEngine = new JexlBuilder().
                loader(new EmptyClassLoader()).
                permissions(JexlPermissions.RESTRICTED.compose("java.time.*", "org.apache.syncope.*")).
                namespaces(Map.of("syncope", new SyncopeJexlFunctions())).
                cache(512).
                silent(false).
                strict(false).
                create();
        jexlTools = new JexlTools(jexlEngine);
        manager = new DefaultNotificationManager(
                derSchemaDAO,
                notificationDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                anyMatchDAO,
                taskDAO,
                relationshipTypeDAO,
                derAttrHandler,
                userDataBinder,
                groupDataBinder,
                anyObjectDataBinder,
                confParamOps,
                entityFactory,
                intAttrNameParser,
                searchCondVisitor,
                jexlTools);
    }

    @Test
    void jxltResolvesWhoAndUserInMapContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("who", "admin");
        UserTO user = new UserTO();
        user.setUsername("deleted-user");
        ctx.put("user", user);
        String out = jexlTools.evaluateTemplate("${who} / ${user.username}", new MapContext(ctx));
        assertFalse(out.contains("${"), out);
        assertEquals("admin / deleted-user", out);
    }

    /**
     * After user deletion the entity is no longer loadable, but {@code before} still holds the
     * {@link UserTO} captured by {@code LogicInvocationHandler}. Notification templates must resolve
     * against that snapshot (SYNCOPE-1744).
     */
    @Test
    void deleteSuccessUsesBeforeUserWhenEntityRemoved() {
        UserTO beforeDelete = new UserTO();
        beforeDelete.setKey("c3b7107b-8886-4b1d-b0e3-2d6bfa6b1f9d");
        beforeDelete.setUsername("deleted-user");
        beforeDelete.getPlainAttrs().add(new Attr.Builder("u_email").value("deleted-user@example.org").build());

        when(userDAO.findById(beforeDelete.getKey())).thenReturn(Optional.empty());

        Notification notification = mock(Notification.class);
        doReturn(Collections.singletonList(notification)).when(notificationDAO).findAll();
        when(notification.isActive()).thenReturn(true);
        when(notification.getEvents()).thenReturn(List.of(DELETE_SUCCESS));
        when(notification.getRecipientsFIQL()).thenReturn(null);
        when(notification.getStaticRecipients()).thenReturn(null);
        when(notification.getRecipientsProvider()).thenReturn(null);
        when(notification.getRecipientAttrName()).thenReturn("email");
        when(notification.getTraceLevel()).thenReturn(TraceLevel.NONE);
        when(notification.getSender()).thenReturn("noreply@syncope.org");
        when(notification.getSubject()).thenReturn("User deleted");

        MailTemplate mailTemplate = mock(MailTemplate.class);
        when(mailTemplate.getTextTemplate()).thenReturn("${user.getPlainAttr(\"u_email\").get().values[0]}");
        when(mailTemplate.getHTMLTemplate()).thenReturn(null);
        when(notification.getTemplate()).thenReturn(mailTemplate);

        when(confParamOps.list(anyString())).thenReturn(Map.of());

        NotificationTask task = mock(NotificationTask.class);
        when(entityFactory.newEntity(NotificationTask.class)).thenReturn(task);
        when(taskDAO.save(any(NotificationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (var auth = mockStatic(AuthContextUtils.class)) {
            auth.when(AuthContextUtils::getDomain).thenReturn(SyncopeConstants.MASTER_DOMAIN);

            manager.createTasks(
                    "admin",
                    OpEvent.CategoryType.LOGIC,
                    "UserLogic",
                    null,
                    "delete",
                    OpEvent.Outcome.SUCCESS,
                    beforeDelete,
                    null);
        }

        ArgumentCaptor<String> textBody = ArgumentCaptor.forClass(String.class);
        verify(task).setTextBody(textBody.capture());
        assertEquals("deleted-user@example.org", textBody.getValue());
    }
}
