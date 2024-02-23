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
package org.apache.syncope.core.persistence.api.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;

public final class ExceptionUtils2 {

    /**
     * Uses commons lang's ExceptionUtils to provide a representation of the full stack trace of the given throwable.
     *
     * @param t throwable to build stack trace from
     * @return a string representation of full stack trace of the given throwable
     */
    public static String getFullStackTrace(final Throwable t) {
        StringBuilder result = new StringBuilder();

        ExceptionUtils.getThrowableList(t).
                forEach(throwable -> result.append(ExceptionUtils.getStackTrace(throwable)).append("\n\n"));

        return result.toString();
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private ExceptionUtils2() {
    }
}
