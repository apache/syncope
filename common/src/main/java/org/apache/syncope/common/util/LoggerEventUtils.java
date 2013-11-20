/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.common.util;

import java.util.AbstractMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.types.AuditElements.Result;

public class LoggerEventUtils {

    public static Map.Entry<EventCategoryTO, Result> parseEventCategory(final String event) {
        final EventCategoryTO eventCategoryTO = new EventCategoryTO();

        Result condition = null;

        if (StringUtils.isNotEmpty(event)) {
            final String[] elements = event.substring(1, event.length() - 1).split("\\]:\\[");

            if (elements.length == 1) {
                eventCategoryTO.setType(EventCategoryType.CUSTOM);
                condition = Result.SUCCESS;
                eventCategoryTO.getEvents().add(event);
            } else {
                EventCategoryType type;

                if (EventCategoryType.PROPAGATION.toString().equals(elements[0])) {
                    type = EventCategoryType.PROPAGATION;
                } else if (EventCategoryType.SYNCHRONIZATION.toString().equals(elements[0])) {
                    type = EventCategoryType.SYNCHRONIZATION;
                } else {
                    try {
                        type = EventCategoryType.valueOf(elements[0]);
                    } catch (Exception e) {
                        type = EventCategoryType.CUSTOM;
                    }
                }

                eventCategoryTO.setType(type);

                eventCategoryTO.setCategory(StringUtils.isNotEmpty(elements[1]) ? elements[1] : null);

                eventCategoryTO.setSubcategory(StringUtils.isNotEmpty(elements[2]) ? elements[2] : null);

                if (elements.length > 3 && StringUtils.isNotEmpty(elements[3])) {
                    eventCategoryTO.getEvents().add(elements[3]);
                }

                if (elements.length > 4) {
                    condition = Result.valueOf(elements[4].toUpperCase());
                }
            }
        }

        return new AbstractMap.SimpleEntry< EventCategoryTO, Result>(eventCategoryTO, condition);
    }

    /**
     * Build event string with the following syntax [type]:[category]:[subcategory]:[event]:[maybe result value cond].
     *
     * @param type event type.
     * @param category event category.
     * @param subcategory event subcategory.
     * @param event event.
     * @param resultValueCondition result value condition.
     * @return event string.
     */
    public static String buildEvent(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final AuditElements.Result resultValueCondition) {

        final StringBuilder eventBuilder = new StringBuilder();

        eventBuilder.append('[');
        if (type != null) {
            if (StringUtils.isNotBlank(type.toString())) {
                eventBuilder.append(type.toString());
            } else {
                eventBuilder.append(type.name());
            }
        }
        eventBuilder.append(']');

        eventBuilder.append(":");

        eventBuilder.append('[');
        if (StringUtils.isNotBlank(category)) {
            eventBuilder.append(category);
        }
        eventBuilder.append(']');

        eventBuilder.append(":");

        eventBuilder.append('[');
        if (StringUtils.isNotBlank(subcategory)) {
            eventBuilder.append(subcategory);
        }
        eventBuilder.append(']');

        eventBuilder.append(":");

        eventBuilder.append('[');
        if (StringUtils.isNotBlank(event)) {
            eventBuilder.append(event);
        }
        eventBuilder.append(']');

        if (resultValueCondition != null) {
            eventBuilder.append(":");

            eventBuilder.append('[');
            eventBuilder.append(resultValueCondition);
            eventBuilder.append(']');
        }

        return eventBuilder.toString();
    }
}
