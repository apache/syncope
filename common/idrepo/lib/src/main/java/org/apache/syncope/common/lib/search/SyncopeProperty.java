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

import org.apache.cxf.jaxrs.ext.search.client.Property;

/**
 * Extension of fluent interface, for {@link org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder}
 * and subclasses.
 *
 * @param <C> the actual complete condition
 */
public interface SyncopeProperty<C extends SyncopeCompleteCondition<?, ?>> extends Property {

    /**
     * Is textual property equal to (ignoring case) given literal or matching given pattern?
     *
     * @param value first value
     * @param moreValues more values
     * @return updated condition
     */
    C equalToIgnoreCase(String value, String... moreValues);

    /**
     * Is textual property different (ignoring case) than given literal or not matching given pattern?
     *
     * @param literalOrPattern The literal or Pattern String
     * @return updated condition
     */
    C notEqualTolIgnoreCase(String literalOrPattern);

    /**
     * Is property null?
     *
     * @return updated condition
     */
    C nullValue();

    /**
     * Is property not null?
     *
     * @return updated condition
     */
    C notNullValue();

    /**
     * Has user, group or any object assigned the given auxiliary class(es)?
     *
     * @param auxClass first auxiliary class
     * @param moreAuxClasses more auxiliary classes
     * @return updated condition
     */
    C hasAuxClasses(String auxClass, String... moreAuxClasses);

    /**
     * Has user, group or any object not assigned the given auxiliary class(es)?
     *
     * @param auxClass first auxiliary class
     * @param moreAuxClasses more auxiliary classes
     * @return updated condition
     */
    C hasNotAuxClasses(String auxClass, String... moreAuxClasses);

    /**
     * Is user, group or any object owning given resource(s)?
     *
     * @param resource first resource
     * @param moreResources more resources
     * @return updated condition
     */
    C hasResources(String resource, String... moreResources);

    /**
     * Is user, group or any object not owning given resource(s)?
     *
     * @param resource first resource
     * @param moreResources more resources
     * @return updated condition
     */
    C hasNotResources(String resource, String... moreResources);

    /**
     * Is user, group or any object in the given dynamic realm(s)?
     *
     * @param dynRealm first dynamic realm
     * @param moreDynRealms more dynamic realms
     * @return updated condition
     */
    C inDynRealms(String dynRealm, String... moreDynRealms);

    /**
     * Is user, group or any object not in the given dynamic realm(s)?
     *
     * @param dynRealm first dynamic realm
     * @param moreDynRealms more dynamic realms
     * @return updated condition
     */
    C notInDynRealms(String dynRealm, String... moreDynRealms);
}
