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
package org.apache.syncope.console.commons;

import org.apache.syncope.common.to.RoleTO;

public class RoleUtils {

    public static RoleTO findRole(final RoleTreeBuilder roleTreeBuilder, final long roleId) {
        RoleTO found = null;
        if (roleTreeBuilder.getAllRoles() != null) {
            for (RoleTO roleTO : roleTreeBuilder.getAllRoles()) {
                if (roleTO.getId() == roleId) {
                    found = roleTO;
                }
            }
        }
        return found;
    }
}
