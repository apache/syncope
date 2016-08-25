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
package org.apache.syncope.client.cli.commands.notification;

import java.util.List;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.NotificationTO;

public class NotificationResultManager extends CommonsResultManager {

    public void printNotifications(final List<NotificationTO> notificationTOs) {
        System.out.println("");
        for (final NotificationTO notificationTO : notificationTOs) {
            printNotification(notificationTO);
        }
    }

    public void printNotification(final NotificationTO notificationTO) {
        System.out.println(" > NOTIFICATION KEY: " + notificationTO.getKey());
        System.out.println("    events: " + notificationTO.getEvents());
        System.out.println("    sender: " + notificationTO.getSender());
        System.out.println("    subject: " + notificationTO.getSubject());
        System.out.println("    recipients: " + notificationTO.getRecipientsFIQL());
        System.out.println("    recipient attribute name: " + notificationTO.getRecipientAttrName());
        System.out.println("    template: " + notificationTO.getTemplate());
        System.out.println("    abouts: " + notificationTO.getAbouts());
        System.out.println("    static recipient: " + notificationTO.getStaticRecipients());
        System.out.println("    trace level: " + notificationTO.getTraceLevel());
        System.out.println("    active: " + notificationTO.isActive());
        System.out.println("    self as recipient: " + notificationTO.isSelfAsRecipient());
        System.out.println("");
    }
}
