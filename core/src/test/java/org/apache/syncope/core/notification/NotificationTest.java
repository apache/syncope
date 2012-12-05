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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.lang.StringUtils;
import org.apache.syncope.controller.UserService;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.rest.AbstractUserTestITCase;
import org.apache.syncope.core.rest.controller.TaskController;
import org.apache.syncope.search.MembershipCond;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.NotificationTaskTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.TraceLevel;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:restContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:schedulingContext.xml",
    "classpath:workflowContext.xml"
})
@Transactional
@Ignore
public class NotificationTest {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationTest.class);

    private static String smtpHost = "localhost";

    private static int smtpPort = 2525;

    private static String pop3Host = "localhost";

    private static int pop3Port = 1110;

    private static String mailAddress = "notificationtest@syncope.apache.org";

    private static String mailPassword = "password";

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
    private ConfDAO confDAO;

    @Autowired
    private UserService userController;

    @Autowired
    private TaskController taskController;

    @Autowired
    private NotificationJob notificationJob;

    @BeforeClass
    public static void startGreenMail() {
        ServerSetup[] config = new ServerSetup[2];
        config[0] = new ServerSetup(smtpPort, smtpHost, ServerSetup.PROTOCOL_SMTP);
        config[1] = new ServerSetup(pop3Port, pop3Host, ServerSetup.PROTOCOL_POP3);
        greenMail = new GreenMail(config);
        greenMail.setUser(mailAddress, mailPassword);
        greenMail.start();
    }

    @AfterClass
    public static void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Before
    public void setupSecurity() {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
        }

        UserDetails userDetails = new User(adminUser, "FAKE_PASSWORD", true, true, true, true, authorities);
        Authentication authentication = new TestingAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Before
    public void setupSMTP() {
        try {
            SyncopeConf smtpHostConf = confDAO.find("smtp.host");
            smtpHostConf.setValue(smtpHost);
            confDAO.save(smtpHostConf);

            SyncopeConf smtpPortConf = confDAO.find("smtp.port");
            smtpPortConf.setValue(Integer.toString(smtpPort));
            confDAO.save(smtpPortConf);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while setting SMTP host and port");
        }

        confDAO.flush();
    }

    private boolean verifyMail(final String sender, final String subject) {
        LOG.info("Waiting for notification to be sent...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        boolean found = false;
        try {
            Session session = Session.getDefaultInstance(System.getProperties());
            Store store = session.getStore("pop3");
            store.connect(pop3Host, pop3Port, mailAddress, mailPassword);

            Folder inbox = store.getFolder("INBOX");
            assertNotNull(inbox);
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            for (Message message : messages) {
                if (sender.equals(message.getFrom()[0].toString()) && subject.equals(message.getSubject())) {
                    found = true;
                    message.setFlag(Flag.DELETED, true);
                }
            }
            inbox.close(true);
            store.close();
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while fetching e-mail");
        }

        return found;
    }

    @Test
    public void notifyByMail() {
        // 1. create suitable notification for subsequent tests
        Notification notification = new Notification();
        notification.addEvent("create");

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        notification.setAbout(NodeCond.getLeafCond(membCond));

        membCond = new MembershipCond();
        membCond.setRoleId(8L);
        notification.setRecipients(NodeCond.getLeafCond(membCond));
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserSchema);

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
        UserTO userTO = AbstractUserTestITCase.getSampleTO(mailAddress);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.addMembership(membershipTO);

        try {
            userController.create(userTO);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while creating");
        }

        // 3. force Quartz job execution and verify e-mail
        try {
            notificationJob.execute(null);
        } catch (SchedulerException e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while triggering notification job");
        }
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id
        Long taskId = null;
        for (NotificationTask task : taskDAO.findAll(NotificationTask.class)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getId();
            }
        }
        assertNotNull(taskId);

        // 5. execute Notification task and verify e-mail
        try {
            taskController.execute(taskId, false);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while executing notification task");
        }
        assertTrue(verifyMail(sender, subject));
    }

    @Test
    public void issueSYNCOPE192() {
        // 1. create suitable notification for subsequent tests
        Notification notification = new Notification();
        notification.addEvent("create");

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        notification.setAbout(NodeCond.getLeafCond(membCond));

        membCond = new MembershipCond();
        membCond.setRoleId(8L);
        notification.setRecipients(NodeCond.getLeafCond(membCond));
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserSchema);

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncope192-" + random.nextLong() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setTraceLevel(TraceLevel.NONE);

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. create user
        UserTO userTO = AbstractUserTestITCase.getSampleTO(mailAddress);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.addMembership(membershipTO);

        try {
            userController.create(userTO);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while creating");
        }

        // 3. force Quartz job execution and verify e-mail
        try {
            notificationJob.execute(null);
        } catch (SchedulerException e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while triggering notification job");
        }
        assertTrue(verifyMail(sender, subject));

        // 4. get NotificationTask id
        Long taskId = null;
        for (NotificationTask task : taskDAO.findAll(NotificationTask.class)) {
            if (sender.equals(task.getSender())) {
                taskId = task.getId();
            }
        }
        assertNotNull(taskId);

        // 5. verify that last exec status was updated
        NotificationTaskTO task = null;
        try {
            task = (NotificationTaskTO) taskController.read(taskId);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while reading notification task");
        }
        assertNotNull(task);
        assertTrue(task.getExecutions().isEmpty());
        assertTrue(task.isExecuted());
        assertTrue(StringUtils.isNotBlank(task.getLatestExecStatus()));
    }
}
