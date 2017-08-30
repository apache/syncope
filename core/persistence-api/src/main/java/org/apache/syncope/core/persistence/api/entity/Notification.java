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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.TraceLevel;

public interface Notification extends Entity {

    List<String> getEvents();

    boolean isSelfAsRecipient();

    void setSelfAsRecipient(boolean selfAsRecipient);

    List<String> getStaticRecipients();

    String getRecipientAttrName();

    void setRecipientAttrName(String recipientAttrName);

    String getRecipientsFIQL();

    void setRecipientsFIQL(String recipientsFIQL);

    Implementation getRecipientsProvider();

    void setRecipientsProvider(Implementation recipientsProvider);

    boolean add(AnyAbout about);

    Optional<? extends AnyAbout> getAbout(AnyType anyType);

    List<? extends AnyAbout> getAbouts();

    String getSender();

    void setSender(String sender);

    String getSubject();

    void setSubject(String subject);

    MailTemplate getTemplate();

    void setTemplate(MailTemplate template);

    TraceLevel getTraceLevel();

    void setTraceLevel(TraceLevel traceLevel);

    boolean isActive();

    void setActive(boolean active);

}
