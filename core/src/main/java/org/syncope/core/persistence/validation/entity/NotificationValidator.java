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
package org.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.Notification;
import org.syncope.types.EntityViolationType;

public class NotificationValidator extends AbstractValidator
        implements ConstraintValidator<NotificationCheck, Notification> {

    @Override
    public void initialize(final NotificationCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(
            final Notification value,
            final ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        if (value.getEvents().isEmpty()) {
            isValid = false;

            context.buildConstraintViolationWithTemplate("No events").addNode(
                    EntityViolationType.InvalidNotification.toString()).
                    addNode("events").addConstraintViolation();
        }
        if (!value.getAbout().checkValidity()) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    "Invalid about search condition").addNode(
                    EntityViolationType.InvalidNotification.toString()).
                    addNode("about").addConstraintViolation();
        }
        if (!value.getRecipients().checkValidity()) {
            isValid = false;

            context.buildConstraintViolationWithTemplate(
                    "Invalid recipients search condition").addNode(
                    EntityViolationType.InvalidNotification.toString()).
                    addNode("recipients").addConstraintViolation();
        }

        return isValid;
    }
}
