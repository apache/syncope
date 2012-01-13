package org.syncope.core.notification;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.annotation.Resource;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.Notification;
import org.syncope.core.persistence.beans.NotificationTask;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.NotificationDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.rest.controller.TaskController;
import org.syncope.core.rest.controller.UserController;
import org.syncope.core.scheduling.NotificationJob;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:restContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:schedulingContext.xml",
    "classpath:workflowContext.xml"
})
@Transactional
public class NotificationTest {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            NotificationTest.class);

    private static String smtpHost;

    private static String pop3Host;

    private static String mailAddress;

    private static String mailPassword;

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
    private UserController userController;

    @Autowired
    private TaskController taskController;

    @Autowired
    private NotificationJob notificationJob;

    @BeforeClass
    public static void loadProperties() {
        Properties props = new Properties();

        try {
            props.load(NotificationTest.class.getResourceAsStream(
                    "/test.properties"));
        } catch (IOException e) {
            LOG.error("Could not load properties", e);
            fail("Could not load test properties");
        }

        smtpHost = props.getProperty("smtp.host");
        pop3Host = props.getProperty("pop3.host");
        mailAddress = props.getProperty("mail.address");
        mailPassword = props.getProperty("mail.password");
        if (StringUtils.isBlank(mailPassword)) {
            throw new IllegalArgumentException(
                    "Empty POP3 password: did you pass -Dmail.password=XXXX?");
        }
    }

    @Before
    public void setupSecurity() {
        List<GrantedAuthority> authorities =
                new ArrayList<GrantedAuthority>();
        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(
                    new GrantedAuthorityImpl(entitlement.getName()));
        }

        UserDetails userDetails = new User(adminUser, "FAKE_PASSWORD",
                true, true, true, true, authorities);
        Authentication authentication = new TestingAuthenticationToken(
                userDetails, "FAKE_PASSWORD", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static UserTO getSampleTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername(email);

        AttributeTO fullnameTO = new AttributeTO();
        fullnameTO.setSchema("fullname");
        fullnameTO.addValue(email);
        userTO.addAttribute(fullnameTO);

        AttributeTO firstnameTO = new AttributeTO();
        firstnameTO.setSchema("firstname");
        firstnameTO.addValue(email);
        userTO.addAttribute(firstnameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Surname");
        userTO.addAttribute(surnameTO);

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue(email);
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue(email);
        userTO.addAttribute(emailTO);

        AttributeTO loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        loginDateTO.addValue(sdf.format(new Date()));
        userTO.addAttribute(loginDateTO);

        // add a derived attribute
        AttributeTO cnTO = new AttributeTO();
        cnTO.setSchema("cn");
        userTO.addDerivedAttribute(cnTO);

        // add a virtual attribute
        AttributeTO virtualdata = new AttributeTO();
        virtualdata.setSchema("virtualdata");
        virtualdata.setValues(Collections.singletonList("virtualvalue"));
        userTO.addVirtualAttribute(virtualdata);

        return userTO;
    }

    private boolean verifyMail(final String sender, final String subject) {
        LOG.info("Waiting for notification to be sent...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        boolean found = false;
        try {
            Session session = Session.getDefaultInstance(
                    System.getProperties());
            Store store = session.getStore("pop3");
            store.connect(pop3Host, mailAddress, mailPassword);

            Folder inbox = store.getFolder("INBOX");
            assertNotNull(inbox);
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            for (int i = 0; i < messages.length; i++) {
                if (sender.equals(messages[i].getFrom()[0].toString())
                        && subject.equals(messages[i].getSubject())) {

                    found = true;
                    messages[i].setFlag(Flag.DELETED, true);
                }
            }
            inbox.close(true);
            store.close();
        } catch (Throwable t) {
            LOG.error("Unexpected exception", t);
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

        Random random = new Random(System.currentTimeMillis());
        String sender = "syncopetest-" + String.valueOf(random.nextLong())
                + "@syncope-idm.org";
        notification.setSender(sender);
        String subject = "Test notification " + random.nextLong();
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);

        notificationDAO.flush();

        // 2. use a real SMTP server
        try {
            SyncopeConf smtpHostConf = confDAO.find("smtp.host");
            smtpHostConf.setValue(smtpHost);
            confDAO.save(smtpHostConf);
        } catch (Throwable t) {
            LOG.error("Unexpected exception", t);
            fail("Unexpected exception while setting SMTP host");
        }

        confDAO.flush();
        confDAO.clear();

        // 3. create user
        UserTO userTO = getSampleTO(mailAddress);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.addMembership(membershipTO);

        try {
            userController.create(new MockHttpServletResponse(), userTO);
        } catch (Throwable t) {
            LOG.error("Unexpected exception", t);
            fail("Unexpected exception while creating");
        }

        // 4. force Quartz job execution and verify e-mail
        try {
            notificationJob.execute(null);
        } catch (SchedulerException e) {
            LOG.error("Unexpected exception", e);
            fail("Unexpected exception while triggering notification job");
        }

        assertTrue(verifyMail(sender, subject));

        List<NotificationTask> tasks = taskDAO.findAll(NotificationTask.class);
        Long taskId = null;
        for (NotificationTask task : tasks) {
            if (sender.equals(task.getSender())) {
                taskId = task.getId();
            }
        }
        assertNotNull(taskId);

        // 5. execute Notification task and verify e-mail
        try {
            taskController.execute(taskId, false);
        } catch (Throwable t) {
            LOG.error("Unexpected exception", t);
            fail("Unexpected exception while executing notification task");
        }

        assertTrue(verifyMail(sender, subject));
    }
}
