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

import java.io.Serializable;
import javax.xml.bind.annotation.XmlEnum;
import org.apache.commons.lang3.StringUtils;

public final class AuditElements implements Serializable {

    private static final long serialVersionUID = -4385059255522273254L;

    public static final String AUTHENTICATION_CATEGORY = "Authentication";

    public static final String LOGIN_EVENT = "login";

    @XmlEnum
    public enum EventCategoryType {

        LOGIC(StringUtils.EMPTY),
        TASK(StringUtils.EMPTY),
        PROPAGATION("PropagationTask"),
        PULL("PullTask"),
        PUSH("PushTask"),
        CUSTOM(StringUtils.EMPTY);

        private final String value;

        EventCategoryType(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @XmlEnum
    public enum Result {

        SUCCESS,
        FAILURE

    }

    private AuditElements() {
        // private constructor for static utility class
    }
}
