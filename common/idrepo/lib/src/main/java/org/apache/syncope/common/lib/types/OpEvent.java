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
package org.apache.syncope.common.lib.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class OpEvent implements BaseBean {

    private static final long serialVersionUID = 7407774670487934097L;

    public enum CategoryType {

        LOGIC,
        WA,
        TASK,
        REPORT,
        PROPAGATION,
        PULL,
        PUSH,
        CUSTOM;

    }

    public enum Outcome {

        SUCCESS,
        FAILURE

    }

    public static final String AUTHENTICATION_CATEGORY = "AUTHENTICATION";

    public static final String LOGIN_OP = "login";

    /**
     * Parse event string into instance.
     *
     * @param event event string
     * @return instance
     */
    @JsonCreator
    public static OpEvent fromString(final String event) {
        if (StringUtils.isBlank(event)) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        String[] elements = event.substring(1, event.length() - 1).split("\\]:\\[");

        CategoryType categoryType;
        String category = null;
        String subCategory = null;
        Outcome outcome = null;
        String op = null;
        if (elements.length == 1) {
            categoryType = CategoryType.CUSTOM;
            outcome = Outcome.SUCCESS;
            op = event;
        } else {
            categoryType = CategoryType.valueOf(elements[0]);

            if (StringUtils.isNotEmpty(elements[1])) {
                category = elements[1];
            }

            if (StringUtils.isNotEmpty(elements[2])) {
                subCategory = elements[2];
            }

            if (elements.length > 3 && StringUtils.isNotEmpty(elements[3])) {
                op = elements[3];
            }

            if (elements.length > 4) {
                outcome = Outcome.valueOf(elements[4].toUpperCase());
            }
        }

        return new OpEvent(categoryType, category, subCategory, op, outcome);
    }

    /**
     * Build event string with the following syntax:
     *
     * <code>
     * [type]:[category]:[subcategory]:[op]:[outcome]
     * </code>
     *
     * @param type event type
     * @param category event category
     * @param subcategory event subcategory
     * @param op operation.
     * @param outcome outcome value condition
     * @return event string
     */
    public static String toString(
            final CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final Outcome outcome) {

        StringBuilder eventBuilder = new StringBuilder();

        eventBuilder.append('[');
        if (type != null) {
            eventBuilder.append(type.name());
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(category)) {
            eventBuilder.append(category);
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(subcategory)) {
            eventBuilder.append(subcategory);
        }
        eventBuilder.append("]:[");
        if (StringUtils.isNotBlank(op)) {
            eventBuilder.append(op);
        }
        eventBuilder.append("]:[");
        if (outcome != null) {
            eventBuilder.append(outcome);
        }
        eventBuilder.append(']');

        return eventBuilder.toString();
    }

    private final CategoryType type;

    private final String category;

    private final String subcategory;

    private final String op;

    private final Outcome outcome;

    @JsonCreator
    public OpEvent(
            @JsonProperty("type") final CategoryType type,
            @JsonProperty("category") final String category,
            @JsonProperty("subcategory") final String subcategory,
            @JsonProperty("op") final String op,
            @JsonProperty("outcome") final Outcome outcome) {

        this.type = Optional.ofNullable(type).orElse(CategoryType.CUSTOM);
        this.category = category;
        this.subcategory = subcategory;
        this.op = op;
        this.outcome = Optional.ofNullable(outcome).orElse(Outcome.SUCCESS);
    }

    public CategoryType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getOp() {
        return op;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(category).
                append(subcategory).
                append(op).
                append(outcome).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OpEvent other = (OpEvent) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(category, other.category).
                append(subcategory, other.subcategory).
                append(op, other.op).
                append(outcome, other.outcome).
                build();
    }

    @Override
    public String toString() {
        return toString(type, category, subcategory, op, outcome);
    }
}
