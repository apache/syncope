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

public interface UserProperty extends SyncopeProperty<UserCompleteCondition> {

    UserCompleteCondition inGroups(String group, String... moreGroups);

    UserCompleteCondition notInGroups(String group, String... moreGroups);

    UserCompleteCondition inRelationships(String anyObject, String... moreAnyObjects);

    UserCompleteCondition notInRelationships(String anyObject, String... moreAnyObjects);

    UserCompleteCondition inRelationshipTypes(String type, String... moreTypes);

    UserCompleteCondition notInRelationshipTypes(String type, String... moreTypes);

    UserCompleteCondition inRoles(String role, String... moreRoles);

    UserCompleteCondition notInRoles(String role, String... moreRoles);
}
