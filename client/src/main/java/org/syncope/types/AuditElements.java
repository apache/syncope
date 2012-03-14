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

import java.util.EnumSet;

public class AuditElements {

    public enum Category {

        authentication,
        configuration,
        connector,
        logger,
        notification,
        policy,
        report,
        resource,
        role,
        schema,
        task,
        user,
        userRequest,
        workflow

    }

    public enum Result {

        success,
        failure

    }

    public static EnumSet<? extends Enum> getSubCategories(final Category category) {
        EnumSet<? extends Enum> result;
        switch (category) {
            case authentication:
                result = EnumSet.allOf(AuthenticationSubCategory.class);
                break;

            default:
                result = null;
        }

        return result;
    }

    public enum AuthenticationSubCategory {

        login,
        getEntitlements

    }
}
