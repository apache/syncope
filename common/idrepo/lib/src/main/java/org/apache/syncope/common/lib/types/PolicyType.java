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

public enum PolicyType {

    /**
     * How username values should look like.
     */
    ACCOUNT,
    /**
     * How password values should look like.
     */
    PASSWORD,
    /**
     * How authentication policies should look like.
     */
    AUTH,
    /**
     * How attribute release policies should look like.
     */
    ATTR_RELEASE,
    /**
     * How access policies should be defined.
     */
    ACCESS,
    /**
     * Controls expiration of access tickets.
     */
    TICKET_EXPIRATION,
    /**
     * For handling propagation behavior.
     */
    PROPAGATION,
    /**
     * For handling conflicts resolution during inbound.
     */
    INBOUND,
    /**
     * For handling conflicts resolution during push.
     */
    PUSH;

}
