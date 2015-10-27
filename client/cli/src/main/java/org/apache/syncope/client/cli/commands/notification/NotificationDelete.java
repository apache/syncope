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

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.view.Messages;
import org.apache.syncope.common.lib.SyncopeClientException;

public class NotificationDelete extends AbstractNotificationCommand {

    private static final String DELETE_HELP_MESSAGE = "notification --delete {NOTIFICATION-ID} {NOTIFICATION-ID} [...]";

    private final Input input;

    public NotificationDelete(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.parameterNumber() >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    notificationSyncopeOperations.delete(parameter);
                    notificationResultManager.deletedMessage("Notification", parameter);
                } catch (final WebServiceException | SyncopeClientException ex) {
                    if (ex.getMessage().startsWith("NotFound")) {
                        notificationResultManager.notFoundError("Notification", parameter);
                    } else {
                        Messages.printMessage(ex.getMessage());
                    }
                } catch (final NumberFormatException ex) {
                    notificationResultManager.notBooleanDeletedError("notification", parameter);
                }
            }
        } else {
            notificationResultManager.commandOptionError(DELETE_HELP_MESSAGE);
        }
    }
}
