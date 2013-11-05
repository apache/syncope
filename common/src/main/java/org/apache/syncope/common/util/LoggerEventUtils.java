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
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.types.AuditElements.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerEventUtils {

    /**
     * Logger.
     */
    private static Logger LOG = LoggerFactory.getLogger(LoggerEventUtils.class);

    public static Map.Entry<EventCategoryTO, Result> parseEventCategory(final String event) {
        final EventCategoryTO eventCategoryTO = new EventCategoryTO();

        Result condition = null;

        if (StringUtils.isNotEmpty(event)) {
            LOG.debug("Parse event {}", event);

            final String[] elements = event.substring(1, event.length() - 1).split("\\]:\\[");

            LOG.debug("Found {} elements", elements.length);

            LOG.debug("Type {}", elements[0]);

            if (EventCategoryType.PROPAGATION.toString().equals(elements[0])) {
                eventCategoryTO.setType(EventCategoryType.PROPAGATION);
            } else if (EventCategoryType.SYNCHRONIZATION.toString().equals(elements[0])) {
                eventCategoryTO.setType(EventCategoryType.SYNCHRONIZATION);
            } else {
                eventCategoryTO.setType(EventCategoryType.valueOf(elements[0]));
            }

            LOG.debug("Category {}", elements[1]);
            eventCategoryTO.setCategory(StringUtils.isNotEmpty(elements[1]) ? elements[1] : null);

            LOG.debug("Sub-category {}", elements[2]);
            eventCategoryTO.setSubcategory(StringUtils.isNotEmpty(elements[2]) ? elements[2] : null);

            if (elements.length > 3 && StringUtils.isNotEmpty(elements[3])) {
                LOG.debug("Event {}", elements[3]);
                eventCategoryTO.getEvents().add(elements[3]);
            }

            if (elements.length > 4) {
                LOG.debug("Result condition {}", elements[4]);
                condition = Result.valueOf(elements[4].toUpperCase());
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
