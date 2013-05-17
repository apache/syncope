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
package org.apache.syncope.common.types;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum TaskType {

    PROPAGATION("propagation"),
    NOTIFICATION("notification"),
    SCHEDULED("sched"),
    SYNCHRONIZATION("sync");

    private String name;

    private TaskType(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TaskType fromString(final String name) {
        if (name != null) {
            for (TaskType t : TaskType.values()) {
                if (t.name.equalsIgnoreCase(name)) {
                    return t;
                }
            }
        }
        return TaskType.valueOf(name.toUpperCase());
    }
}
