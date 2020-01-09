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
package org.apache.syncope.core.provisioning.api;

import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.common.lib.types.AuditElements;

public interface AuditManager {

    /**
     * Checks if audit is requested matching the provided conditions.
     *
     * @param who user triggering the event
     * @param type event category type
     * @param category event category
     * @param subcategory event subcategory
     * @param event event
     * @return created notification tasks
     */
    boolean auditRequested(
            String who,
            AuditElements.EventCategoryType type,
            String category,
            String subcategory,
            String event);

    /**
     * Create audit entries according to the provided event.
     *
     * @param event Spring event raised during Logic processing
     */
    void audit(AfterHandlingEvent event);

    /**
     * Create audit entries for each audit matching provided conditions.
     *
     * @param who user triggering the event
     * @param type event category type
     * @param category event category
     * @param subcategory event subcategory
     * @param event event
     * @param condition result value condition.
     * @param before object(s) available before the event
     * @param output object(s) produced by the event
     * @param input object(s) provided to the event
     */
    @SuppressWarnings("squid:S00107")
    void audit(
            String who,
            AuditElements.EventCategoryType type,
            String category,
            String subcategory,
            String event,
            AuditElements.Result condition,
            Object before,
            Object output,
            Object... input);
}
