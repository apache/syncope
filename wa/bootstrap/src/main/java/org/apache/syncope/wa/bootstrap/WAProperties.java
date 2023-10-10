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
package org.apache.syncope.wa.bootstrap;

import org.apache.syncope.common.lib.SyncopeProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wa")
public class WAProperties extends SyncopeProperties {

    private static final long serialVersionUID = 7925827623055998239L;

    private int contextRefreshDelay = 15;

    public int getContextRefreshDelay() {
        return contextRefreshDelay;
    }

    public void setContextRefreshDelay(final int contextRefreshDelay) {
        this.contextRefreshDelay = contextRefreshDelay;
    }
}
