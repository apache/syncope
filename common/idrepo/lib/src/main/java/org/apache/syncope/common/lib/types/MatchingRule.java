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

/**
 * Pull/Push task matching rule.
 */
public enum MatchingRule {

    /**
     * Do not perform any action.
     */
    IGNORE,
    /**
     * Update matching entity.
     */
    UPDATE,
    /**
     * Delete resource entity.
     */
    DEPROVISION,
    /**
     * Unlink resource and delete resource entity.
     */
    UNASSIGN,
    /**
     * Just unlink resource without performing any (de-)provisioning operation.
     */
    UNLINK,
    /**
     * Just link resource without performing any (de-)provisioning operation.
     */
    LINK;

    public static String toOp(final MatchingRule rule) {
        return new StringBuilder(MatchingRule.class.getSimpleName()).
                append('_').append(rule.name()).toString().toLowerCase();
    }
}
