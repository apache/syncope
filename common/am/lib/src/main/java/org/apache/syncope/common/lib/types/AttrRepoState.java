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
package org.apache.syncope.common.lib.types;

public enum AttrRepoState {
    /**
     * Active and enabled repository,
     * and is invoked by default automatically.
     */
    ACTIVE,
    /**
     * Attribute repository is disabled and will not be used
     * to resolve people and attributes.
     */
    DISABLED,
    /**
     * Repository is in a semi-enabled state,
     * waiting to be called only on-demand when explicitly
     * asked for and will not be registered into the resolution plan.
     */
    STANDBY

}
