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
package org.apache.syncope.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.syncope.cli.SyncopeServices;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.NotificationTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(
        commandNames = "notification",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope notification service")
public class NotificationCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationCommand.class);

    private static final Class SYNCOPE_NOTIFICATION_CLASS = NotificationService.class;

    private final String helpMessage = "Usage: notification [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -r, --read \n"
            + "       Syntax: -r={NOTIFICATION-ID} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={NOTIFICATION-ID}";

    @Parameter(names = {"-r", "--read"})
    public Long notificationIdToRead = -1L;

    @Parameter(names = {"-d", "--delete"})
    public Long notificationIdToDelete = -1L;

    @Override
    public void execute() {
        final NotificationService notificationService = ((NotificationService) SyncopeServices.get(
                SYNCOPE_NOTIFICATION_CLASS));

        LOG.debug("Notification service successfully created");

        if (help) {
            LOG.debug("- notification help command");
            System.out.println(helpMessage);
        } else if (list) {
            LOG.debug("- notification list command");
            try {
                for (final NotificationTO notificationTO : notificationService.list()) {
                    System.out.println(notificationTO);
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (notificationIdToRead > -1L) {
            LOG.debug("- notification read {} command", notificationIdToRead);
            try {
                System.out.println(notificationService.read(notificationIdToRead));
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (notificationIdToDelete > -1L) {
            try {
                LOG.debug("- notification delete {} command", notificationIdToDelete);
                notificationService.delete(notificationIdToDelete);
                System.out.println(" - Notification " + notificationIdToDelete + " deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}
