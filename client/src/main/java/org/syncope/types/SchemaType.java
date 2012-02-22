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
package org.syncope.types;

public enum SchemaType {

    String("java.lang.String"),
    Long("java.lang.Long"),
    Double("java.lang.Double"),
    Boolean("java.lang.Boolean"),
    Date("java.util.Date"),
    Enum("java.lang.Enum");

    final private String className;

    SchemaType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public boolean isConversionPatternNeeded() {
        return this == SchemaType.Date
                || this == SchemaType.Double
                || this == SchemaType.Long;
    }
}
