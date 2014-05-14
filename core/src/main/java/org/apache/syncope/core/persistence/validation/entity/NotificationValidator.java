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
package org.apache.syncope.core.persistence.validation.entity;

import java.util.regex.Matcher;
import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.Notification;

public class NotificationValidator extends AbstractValidator<NotificationCheck, Notification> {

    @Override
    public boolean isValid(final Notification value, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (value.getEvents().isEmpty()) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidNotification, "No events")).
                    addNode("events").addConstraintViolation();
        }

        if (!value.getStaticRecipients().isEmpty()) {
            for (String mail : value.getStaticRecipients()) {
                Matcher matcher = SyncopeConstants.EMAIL_PATTERN.matcher(mail);
                if (!matcher.matches()) {
                    LOG.error("Invalid mail address: {}", mail);
                    isValid = false;

                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidNotification, "Invalid mail address: " + mail)).
                            addNode("staticRecipients").addConstraintViolation();
                }
            }
        }

        return isValid;
    }
}
