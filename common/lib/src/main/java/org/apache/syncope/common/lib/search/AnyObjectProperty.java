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

import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;

public interface AnyObjectProperty extends SyncopeProperty {

    CompleteCondition inGroups(String group, String... moreGroups);

    CompleteCondition notInGroups(String group, String... moreGroups);

    CompleteCondition inRelationships(String anyObject, String... moreAnyObjects);

    CompleteCondition notInRelationships(String anyObject, String... moreAnyObjects);

    CompleteCondition inRelationshipTypes(String type, String... moreTypes);

    CompleteCondition notInRelationshipTypes(String type, String... moreTypes);

    CompleteCondition isAssignable();

}
