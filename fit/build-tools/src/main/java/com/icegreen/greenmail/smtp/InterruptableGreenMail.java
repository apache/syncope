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
package com.icegreen.greenmail.smtp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.configuration.ConfiguredGreenMail;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that manages a greenmail server with support for multiple protocols.
 */
public class InterruptableGreenMail extends ConfiguredGreenMail {

    protected static final Logger LOG = LoggerFactory.getLogger(InterruptableGreenMail.class);

    protected Managers managers;

    protected Map<String, AbstractServer> services;

    protected ServerSetup[] config;

    /**
     * Creates a SMTP, SMTPS, POP3, POP3S, IMAP, and IMAPS server binding onto non-default ports.
     * The ports numbers are defined in {@link ServerSetupTest}
     */
    public InterruptableGreenMail() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Call this constructor if you want to run one of the email servers only
     *
     * @param config Server setup to use
     */
    public InterruptableGreenMail(final ServerSetup config) {
        this(new ServerSetup[] { config });
    }

    /**
     * Call this constructor if you want to run more than one of the email servers
     *
     * @param config Server setup to use
     */
    public InterruptableGreenMail(final ServerSetup[] config) {
        this.config = config;
        init();
    }

    /**
     * Initialize
     */
    protected void init() {
        if (managers == null) {
            managers = new Managers();
        }
        if (services == null) {
            services = createServices(config, managers);
        }
    }

    @Override
    public synchronized void start() {
        init();

        final Collection<AbstractServer> servers = services.values();
        servers.forEach(service -> {
            service.startService();
        });

        // Wait till all services are up and running
        servers.forEach(service -> {
            try {
                service.waitTillRunning(service.getServerSetup().getServerStartupTimeout());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Could not start mail service " + service, ex);
            }
        });

        LOG.debug("Started services, performing check if all up");
        // Make sure if all services are up in a second loop, giving slow services more time.
        servers.stream().
                filter(service -> (!service.isRunning())).
                forEach((service) -> {
                    throw new IllegalStateException("Could not start mail server " + service
                            + ", try to set server startup timeout > "
                            + service.getServerSetup().getServerStartupTimeout()
                            + " via " + ServerSetup.class.getSimpleName() + ".setServerStartupTimeout(timeoutInMs) or "
                            + "-Dgreenmail.startup.timeout");
                });

        doConfigure();
    }

    @Override
    public synchronized void stop() {
        LOG.debug("Stopping GreenMail ...");

        if (services != null) {
            services.values().forEach(service -> {
                LOG.debug("Stopping service {}", service);
                service.stopService();
            });
        }
        managers = new Managers();
        services = null;
    }

    @Override
    public void reset() {
        stop();
        start();
    }

    /**
     * Create the required services according to the server setup
     *
     * @param config Service configuration
     * @param mgr Service managers
     * @return Services map
     */
    protected Map<String, AbstractServer> createServices(final ServerSetup[] config, final Managers mgr) {
        Map<String, AbstractServer> srvc = new HashMap<>();
        for (ServerSetup setup : config) {
            if (srvc.containsKey(setup.getProtocol())) {
                throw new IllegalArgumentException("Server '" + setup.getProtocol()
                        + "' was found at least twice in the array");
            }
            final String protocol = setup.getProtocol();
            if (protocol.startsWith(ServerSetup.PROTOCOL_SMTP)) {
                srvc.put(protocol, new InterruptableSmtpServer(setup, mgr));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_POP3)) {
                srvc.put(protocol, new Pop3Server(setup, mgr));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_IMAP)) {
                srvc.put(protocol, new ImapServer(setup, mgr));
            }
        }
        return srvc;
    }

    @Override
    public SmtpServer getSmtp() {
        return (SmtpServer) services.get(ServerSetup.PROTOCOL_SMTP);
    }

    @Override
    public ImapServer getImap() {
        return (ImapServer) services.get(ServerSetup.PROTOCOL_IMAP);

    }

    @Override
    public Pop3Server getPop3() {
        return (Pop3Server) services.get(ServerSetup.PROTOCOL_POP3);
    }

    @Override
    public SmtpServer getSmtps() {
        return (SmtpServer) services.get(ServerSetup.PROTOCOL_SMTPS);
    }

    @Override
    public ImapServer getImaps() {
        return (ImapServer) services.get(ServerSetup.PROTOCOL_IMAPS);

    }

    @Override
    public Pop3Server getPop3s() {
        return (Pop3Server) services.get(ServerSetup.PROTOCOL_POP3S);
    }

    @Override
    public Managers getManagers() {
        return managers;
    }

    //~ Convenience Methods, often needed while testing ---------------------------------------------------------------
    @Override
    public boolean waitForIncomingEmail(final long timeout, final int emailCount) {
        final CountDownLatch waitObject = managers.getSmtpManager().createAndAddNewWaitObject(emailCount);
        final long endTime = System.currentTimeMillis() + timeout;
        while (waitObject.getCount() > 0) {
            final long waitTime = endTime - System.currentTimeMillis();
            if (waitTime < 0L) {
                return waitObject.getCount() == 0;
            }
            try {
                waitObject.await(waitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Continue loop, in case of premature interruption
            }
        }
        return waitObject.getCount() == 0;
    }

    @Override
    public boolean waitForIncomingEmail(final int emailCount) {
        return waitForIncomingEmail(5000L, emailCount);
    }

    @Override
    public MimeMessage[] getReceivedMessages() {
        List<StoredMessage> msgs = managers.getImapHostManager().getAllMessages();
        MimeMessage[] ret = new MimeMessage[msgs.size()];
        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage storedMessage = msgs.get(i);
            ret[i] = storedMessage.getMimeMessage();
        }
        return ret;
    }

    @Deprecated
    @Override
    public MimeMessage[] getReceviedMessagesForDomain(final String domain) {
        return getReceivedMessagesForDomain(domain);
    }

    @Override
    public MimeMessage[] getReceivedMessagesForDomain(final String domain) {
        List<StoredMessage> msgs = managers.getImapHostManager().getAllMessages();
        List<MimeMessage> ret = new ArrayList<>();
        try {
            for (StoredMessage msg : msgs) {
                String tos = GreenMailUtil.getAddressList(msg.getMimeMessage().getAllRecipients());
                if (tos.toLowerCase().contains(domain)) {
                    ret.add(msg.getMimeMessage());
                }
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return ret.toArray(new MimeMessage[ret.size()]);
    }

    @Override
    public GreenMailUser setUser(final String login, final String password) {
        return setUser(login, login, password);
    }

    @Override
    public GreenMailUser setUser(final String email, final String login, final String password) {
        GreenMailUser user = managers.getUserManager().getUser(login);
        if (null == user) {
            try {
                user = managers.getUserManager().createUser(email, login, password);
            } catch (UserException e) {
                throw new RuntimeException(e);
            }
        } else {
            user.setPassword(password);
        }
        return user;
    }

    @Override
    public void setQuotaSupported(final boolean isEnabled) {
        managers.getImapHostManager().getStore().setQuotaSupported(isEnabled);
    }

    @Override
    public void setUsers(final Properties users) {
        users.keySet().stream().
                map(String.class::cast).
                forEach(email -> {
                    String password = users.getProperty(email);
                    setUser(email, email, password);
                });
    }

    @Override
    public InterruptableGreenMail withConfiguration(final GreenMailConfiguration config) {
        // Just overriding to return more specific type
        super.withConfiguration(config);
        return this;
    }

    @Override
    public void purgeEmailFromAllMailboxes() throws FolderException {
        ImapHostManager imaphost = getManagers().getImapHostManager();
        InMemoryStore store = (InMemoryStore) imaphost.getStore();
        Collection<MailFolder> mailboxes = store.listMailboxes("*");
        mailboxes.forEach(folder -> {
            folder.deleteAllMessages();
        });
    }
}
