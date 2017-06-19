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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertNotNull;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractNotificationTaskITCase extends AbstractTaskITCase {

    private static final String POP3_HOST = "localhost";

    private static final int POP3_PORT = 1110;

    private static String SMTP_HOST;

    private static int SMTP_PORT;

    private static GreenMail greenMail;

    @BeforeClass
    public static void startGreenMail() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = ExceptionMapperITCase.class.getResourceAsStream("/mail.properties");
            props.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /mail.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        SMTP_HOST = props.getProperty("smtpHost");
        assertNotNull(SMTP_HOST);
        SMTP_PORT = Integer.parseInt(props.getProperty("smtpPort"));
        assertNotNull(SMTP_PORT);

        ServerSetup[] config = new ServerSetup[2];
        config[0] = new ServerSetup(SMTP_PORT, SMTP_HOST, ServerSetup.PROTOCOL_SMTP);
        config[1] = new ServerSetup(POP3_PORT, POP3_HOST, ServerSetup.PROTOCOL_POP3);
        greenMail = new GreenMail(config);
        greenMail.start();
    }

    @AfterClass
    public static void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    protected boolean verifyMail(final String sender, final String subject, final String mailAddress) throws Exception {
        LOG.info("Waiting for notification to be sent...");
        greenMail.waitForIncomingEmail(1);

        boolean found = false;
        Session session = Session.getDefaultInstance(System.getProperties());
        session.setDebug(true);
        Store store = session.getStore("pop3");
        store.connect(POP3_HOST, POP3_PORT, mailAddress, mailAddress);

        Folder inbox = store.getFolder("INBOX");
        assertNotNull(inbox);
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();
        for (Message message : messages) {
            if (sender.equals(message.getFrom()[0].toString()) && subject.equals(message.getSubject())) {
                found = true;
                message.setFlag(Flags.Flag.DELETED, true);
            }
        }

        inbox.close(true);
        store.close();
        return found;
    }

}
