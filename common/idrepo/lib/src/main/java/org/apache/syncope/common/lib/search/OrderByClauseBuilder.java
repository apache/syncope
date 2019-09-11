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
package org.apache.syncope.common.lib.search;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple builder for generating {@code orderby} values.
 */
public class OrderByClauseBuilder {

    private final StringBuilder builder = new StringBuilder();

    public OrderByClauseBuilder asc(final String key) {
        builder.append(key).append(" ASC,");
        return this;
    }

    public OrderByClauseBuilder desc(final String key) {
        builder.append(key).append(" DESC,");
        return this;
    }

    public String build() {
        return builder.length() == 0
                ? StringUtils.EMPTY
                : builder.deleteCharAt(builder.length() - 1).toString();
    }
}
