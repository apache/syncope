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
package org.apache.syncope.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Resource;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.RoleFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.logic.notification.NotificationJob;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:provisioningContext.xml",
    "classpath:logicContext.xml",
    "classpath:workflowContext.xml",
    "classpath:persistenceTest.xml",
    "classpath:logicTest.xml"
})
@Transactional
public class NotificationTest {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationTest.class);

    private static final String SMTP_HOST = "localhost";

    private static final int SMTP_PORT = 2525;

    private static final String POP3_HOST = "localhost";

    private static final int POP3_PORT = 1110;

    private static final String MAIL_ADDRESS = "notificationtest@syncope.apache.org";

    private static final String MAIL_PASSWORD = "password";

    private static GreenMail greenMail;

    @Resource(name = "adminUser")
    private String adminUser;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private UserLogic userLogic;

    @Autowired
    private RoleLogic roleLogic;

    @Autowired
    private TaskLogic taskLogic;

    @Autowired
    private NotificationJob notificationJob;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    @BeforeClass
    public static void startGreenMail() {
        ServerSetup[] config = new ServerSetup[2];
        config[0] = new ServerSetup(SMTP_PORT, SMTP_HOST, ServerSetup.PROTOCOL_SMTP);
        config[1] = new ServerSetup(POP3_PORT, POP3_HOST, ServerSetup.PROTOCOL_POP3);
        greenMail = new GreenMail(config);
        greenMail.setUser(MAIL_ADDRESS, MAIL_PASSWORD);
        greenMail.start();
    }

    @AfterClass
    public static void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    private static UserTO getUniqueSampleTO(final String email) {
        return getSampleTO(UUID.randomUUID().toString().substring(0, 8) + email);
    }

    private static AttrTO attributeTO(final String schema, final String value) {
        AttrTO attr = new AttrTO();
        attr.setSchema(schema);
        attr.getValues().add(value);
        return attr;
    }

    private static UserTO getSampleTO(final String email) {
        String uid = email;
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername(uid);

        userTO.getPlainAttrs().add(attributeTO("fullname", uid));
        userTO.getPlainAttrs().add(attributeTO("firstname", uid));
        userTO.getPlainAttrs().add(attributeTO("surname", "surname"));
        userTO.getPlainAttrs().add(attributeTO("type", "a type"));
        userTO.getPlainAttrs().add(attributeTO("userId", uid));
        userTO.getPlainAttrs().add(attributeTO("email", uid));
        userTO.getPlainAttrs().add(attributeTO("loginDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
        userTO.getDerAttrs().add(attributeTO("cn", null));
        userTO.getVirAttrs().add(attributeTO("virtualdata", "virtualvalue"));
        return userTO;
    }

    @Before
    public void setupSecurity() {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new SimpleGrantedAuthority(entitlement.getKey()));
        }

        UserDetails userDetails = new User(adminUser, "FAKE_PASSWORD", true, true, true, true, authorities);
        Authentication authentication = new TestingAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Before
    public void setupSMTP() throws Exception {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
        sender.setDefaultEncoding(SyncopeConstants.DEFAULT_ENCODING);
        sender.setHost(SMTP_HOST);
        sender.setPort(SMTP_PORT);
    }

    private boolean verifyMail(final String sender, final String subject) throws Exception {
        LOG.info("Waiting for notification to be sent...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        boolean found = false;
        Session session = Session.getDefaultInstance(System.getProperties());
        Store store = session.getStore("pop3");
        store.connect(POP3_HOST, POP3_PORT, MAIL_ADDRESS, MAIL_PASSWORD);

        Folder inbox = store.getFolder("INBOX");
        assertNotNull(inbox);
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();
        for (int i = 0; i < messages.length; i++) {
            if (sender.equals(messages[i].getFrom()[0].toString()) && subject.equals(messages[i].getSubject())) {
                found = true;
                messages[i].setFlag(Flag.DELETED, true);
            }
        }

        inbox.close(true);
        store.close();
        return found;
    }

    @Test
    public void notifyByMail() throws Exception {
        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(new UserFiqlSearchConditionBuilder().hasRoles(7L).query());
        notification.setRecipients(new UserFiqlSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create user
        UserTO userTO = getSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. force Quartz job execution and verify e-mail
        notificationJob.execute(null);
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id and text body
        Long taskId = null;
        String textBody = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getKey();
                textBody = task.getTextBody();
            }
        }
        assertNotNull(taskId);
        assertNotNull(textBody);
        assertTrue("Notification mail text doesn't contain expected content.",
                textBody.contains("Your email address is notificationtest@syncope.apache.org."));
        assertTrue("Notification mail text doesn't contain expected content.",
                textBody.contains("Your email address inside a link: "
                        + "http://localhost/?email=notificationtest%40syncope.apache.org ."));

        // 5. execute Notification task and verify e-mail
        taskLogic.execute(taskId, false);
        assertTrue(verifyMail(sender, subject));
    }

    @Test
    public void issueSYNCOPE192() throws Exception {
        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(new UserFiqlSearchConditionBuilder().hasRoles(7L).query());
        notification.setRecipients(new UserFiqlSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncope192-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setTraceLevel(TraceLevel.NONE);

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        // 2. create user
        UserTO userTO = getSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. force Quartz job execution and verify e-mail
        notificationJob.execute(null);
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id
        Long taskId = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getKey();
            }
        }
        assertNotNull(taskId);

        // 5. verify that last exec status was updated
        NotificationTaskTO task = (NotificationTaskTO) taskLogic.read(taskId);
        assertNotNull(task);
        assertTrue(task.getExecutions().isEmpty());
        assertTrue(task.isExecuted());
        assertTrue(StringUtils.isNotBlank(task.getLatestExecStatus()));
    }

    @Test
    public void notifyByMailEmptyAbout() throws Exception {
        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(null);
        notification.setRecipients(new UserFiqlSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create user
        UserTO userTO = getSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. force Quartz job execution and verify e-mail
        notificationJob.execute(null);
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id
        Long taskId = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getKey();
            }
        }
        assertNotNull(taskId);

        // 5. execute Notification task and verify e-mail
        taskLogic.execute(taskId, false);
        assertTrue(verifyMail(sender, subject));
    }

    @Test
    public void notifyByMailWithRetry() throws Exception {
        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(null);
        notification.setRecipients(new UserFiqlSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create user
        UserTO userTO = getSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. Set number of retries
        CPlainAttr maxRetries = entityFactory.newEntity(CPlainAttr.class);
        maxRetries.setSchema(plainSchemaDAO.find("notification.maxRetries", CPlainSchema.class));
        maxRetries.addValue("5", attrUtilFactory.getInstance(AttributableType.CONFIGURATION));
        confDAO.save(maxRetries);
        confDAO.flush();

        // 4. Stop mail server to force error sending mail
        stopGreenMail();

        // 5. force Quartz job execution multiple times
        for (int i = 0; i < 10; i++) {
            notificationJob.execute(null);
        }

        // 6. get NotificationTask, count number of executions
        NotificationTask foundTask = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                foundTask = task;
            }
        }
        assertNotNull(foundTask);
        assertEquals(6, notificationManager.countExecutionsWithStatus(foundTask.getKey(),
                NotificationJob.Status.NOT_SENT.name()));

        // 7. start mail server again
        startGreenMail();

        // 8. reset number of retries
        maxRetries = entityFactory.newEntity(CPlainAttr.class);
        maxRetries.setSchema(plainSchemaDAO.find("notification.maxRetries", CPlainSchema.class));
        maxRetries.addValue("0", attrUtilFactory.getInstance(AttributableType.CONFIGURATION));
        confDAO.save(maxRetries);
        confDAO.flush();
    }

    @Test
    public void issueSYNCOPE445() throws Exception {
        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(new UserFiqlSearchConditionBuilder().hasRoles(7L).query());
        notification.setRecipients(new UserFiqlSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        notification.getStaticRecipients().add("syncope445@syncope.apache.org");

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create user
        UserTO userTO = getSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. force Quartz job execution and verify e-mail
        notificationJob.execute(null);
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id and text body
        Long taskId = null;
        String textBody = null;
        Set<String> recipients = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getKey();
                textBody = task.getTextBody();
                recipients = task.getRecipients();
            }
        }

        assertNotNull(taskId);
        assertNotNull(textBody);
        assertTrue(recipients.contains("syncope445@syncope.apache.org"));

        // 5. execute Notification task and verify e-mail
        taskLogic.execute(taskId, false);
        assertTrue(verifyMail(sender, subject));
    }

    @Test
    public void issueSYNCOPE492() throws Exception {
        // 1. create suitable disabled notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");
        notification.setUserAbout(new UserFiqlSearchConditionBuilder().hasRoles(7L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        notification.getStaticRecipients().add("syncope492@syncope.apache.org");

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(false);

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        final int tasksNumberBefore = taskDAO.findAll(TaskType.NOTIFICATION).size();

        // 2. create user
        UserTO userTO = getUniqueSampleTO(MAIL_ADDRESS);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userLogic.create(userTO, true);

        // 3. force Quartz job execution
        notificationJob.execute(null);

        // 4. check if number of tasks is not incremented
        assertEquals(tasksNumberBefore, taskDAO.findAll(TaskType.NOTIFICATION).size());
    }

    @Test
    public void issueSYNCOPE446() throws Exception {

        // 1. create suitable notification for subsequent tests
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.addEvent("[REST]:[RoleLogic]:[]:[create]:[SUCCESS]");
        notification.setRoleAbout(new RoleFiqlSearchConditionBuilder().is("name").equalTo("role446").query());
        notification.setSelfAsRecipient(false);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        notification.getStaticRecipients().add(MAIL_ADDRESS);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create role
        RoleTO roleTO = new RoleTO();
        roleTO.setName("role446");
        roleTO.setParent(1L);

        RoleTO createdRole = roleLogic.create(roleTO);
        assertNotNull(createdRole);

        // 3. force Quartz job execution and verify e-mail
        notificationJob.execute(null);
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id and text body
        Long taskId = null;
        String textBody = null;
        Set<String> recipients = null;
        for (NotificationTask task : taskDAO.<NotificationTask>findAll(TaskType.NOTIFICATION)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getKey();
                textBody = task.getTextBody();
                recipients = task.getRecipients();
            }
        }

        assertNotNull(taskId);
        assertNotNull(textBody);
        assertTrue(recipients != null && recipients.contains(MAIL_ADDRESS));

        // 5. execute Notification task and verify e-mail
        taskLogic.execute(taskId, false);
        assertTrue(verifyMail(sender, subject));
    }
}
