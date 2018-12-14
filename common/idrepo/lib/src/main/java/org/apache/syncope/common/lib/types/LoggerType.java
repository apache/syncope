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

import javax.xml.bind.annotation.XmlEnum;
import org.apache.commons.lang3.StringUtils;

@XmlEnum
public enum LoggerType {

    /**
     * This type describes a common logger used to handle system and application events.
     */
    LOG(StringUtils.EMPTY),
    /**
     * Audit logger only focus on security related events, usually logging how did what and when.
     * In case of a security incident audit loggers should allow an administrator to recall all
     * actions a certain user has done.
     */
    AUDIT("syncope.audit");

    private final String prefix;

    LoggerType(final String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
