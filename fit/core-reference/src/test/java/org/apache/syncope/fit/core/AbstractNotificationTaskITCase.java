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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractNotificationTaskITCase extends AbstractTaskITCase {

    private static final String POP3_HOST = "localhost";

    private static int POP3_PORT;

    @BeforeAll
    public static void conf() {
        Properties props = new Properties();
        try (InputStream propStream = AbstractNotificationTaskITCase.class.getResourceAsStream("/test.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }

        POP3_PORT = Integer.parseInt(props.getProperty("testmail.pop3port"));
        assertNotNull(POP3_PORT);
    }

    private static boolean pop3(final String sender, final String subject, final String mailAddress) throws Exception {
        boolean found = false;
        try (Store store = Session.getDefaultInstance(System.getProperties()).getStore("pop3")) {
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
        }
        return found;
    }

    protected static void verifyMail(
            final String sender,
            final String subject,
            final String mailAddress,
            final int maxWaitSeconds) throws Exception {

        Mutable<Boolean> read = new MutableObject<>(false);
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                read.setValue(pop3(sender, subject, mailAddress));
                return read.getValue();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
