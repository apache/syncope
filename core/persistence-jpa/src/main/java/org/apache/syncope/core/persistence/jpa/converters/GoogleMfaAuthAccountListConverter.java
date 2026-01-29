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
<<<<<<<< HEAD:core/persistence-jpa/src/main/java/org/apache/syncope/core/persistence/jpa/converters/GoogleMfaAuthAccountListConverter.java
package org.apache.syncope.core.persistence.jpa.converters;

import jakarta.persistence.Converter;
import java.util.List;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import tools.jackson.core.type.TypeReference;

@Converter
public class GoogleMfaAuthAccountListConverter extends SerializableListConverter<GoogleMfaAuthAccount> {

    protected static final TypeReference<List<GoogleMfaAuthAccount>> TYPEREF =
            new TypeReference<List<GoogleMfaAuthAccount>>() {
    };

    @Override
    protected TypeReference<List<GoogleMfaAuthAccount>> typeRef() {
        return TYPEREF;
    }
========
package org.apache.syncope.core.provisioning.api.pushpull;

import org.apache.syncope.core.persistence.api.entity.Any;

public interface SyncopeAnyPushResultHandler extends SyncopePushResultHandler {

    boolean handle(Any any);
>>>>>>>> upstream/master:core/provisioning-api/src/main/java/org/apache/syncope/core/provisioning/api/pushpull/SyncopeAnyPushResultHandler.java
}
