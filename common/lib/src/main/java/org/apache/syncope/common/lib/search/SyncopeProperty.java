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
import org.apache.cxf.jaxrs.ext.search.client.Property;

/**
 * Extension of fluent interface, for {@link org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder}
 * and subclasses.
 */
public interface SyncopeProperty extends Property {

    /** Is textual property equal to (ignoring case) given literal or matching given pattern? */
    CompleteCondition equalToIgnoreCase(String value, String... moreValues);

    /** Is textual property different (ignoring case) than given literal or not matching given pattern? */
    CompleteCondition notEqualTolIgnoreCase(String literalOrPattern);

    /** Is property null? */
    CompleteCondition nullValue();

    /** Is property not null? */
    CompleteCondition notNullValue();

    /** Is user, group or any object owning given resource(s)? */
    CompleteCondition hasResources(String resource, String... moreResources);

    /** Is user, group or any object not owning given resource(s)? */
    CompleteCondition hasNotResources(String resource, String... moreResources);

}
