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
package org.apache.syncope.common.search;

import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.client.Property;

/**
 * Extension of fluent interface, for {@link SyncopeFiqlSearchConditionBuilder}.
 */
public interface SyncopeProperty extends Property {

    CompleteCondition nullValue();

    CompleteCondition notNullValue();

    CompleteCondition hasRoles(Long role, Long... moreRoles);

    CompleteCondition hasNotRoles(Long role, Long... moreRoles);

    CompleteCondition hasResources(String resource, String... moreResources);

    CompleteCondition hasNotResources(String resource, String... moreResources);

    CompleteCondition hasEntitlements(String entitlement, String... moreEntitlements);

    CompleteCondition hasNotEntitlements(String entitlement, String... moreEntitlements);

}
