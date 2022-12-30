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
package org.apache.syncope.fit.buildtools;

import com.icegreen.greenmail.util.InterruptableGreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utility servlet context listener managing GreenMail test server instance.
 */
@WebListener
public class GreenMailStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(GreenMailStartStopListener.class);

    public static final String GREENMAIL = "greenMail";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sc);

        InterruptableGreenMail greenMail = (InterruptableGreenMail) sc.getAttribute(GREENMAIL);
        if (greenMail == null) {
            ServerSetup[] config = new ServerSetup[2];
            config[0] = new ServerSetup(
                    ctx.getEnvironment().getProperty("testmail.smtpport", Integer.class),
                    "localhost", ServerSetup.PROTOCOL_SMTP);
            config[1] = new ServerSetup(
                    ctx.getEnvironment().getProperty("testmail.pop3port", Integer.class),
                    "localhost", ServerSetup.PROTOCOL_POP3);
            greenMail = new InterruptableGreenMail(config);
            greenMail.start();

            sc.setAttribute(GREENMAIL, greenMail);
        }

        LOG.info("SMTP and POP3 servers successfully (re)started");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();

        InterruptableGreenMail greenMail = (InterruptableGreenMail) sc.getAttribute(GREENMAIL);
        if (greenMail != null) {
            greenMail.stop();

            LOG.info("SMTP and POP3 servers successfully stopped");
        }
    }
}
