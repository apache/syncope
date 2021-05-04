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
package org.apache.syncope.common.keymaster.client.zookeeper;

import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingDomainWatcher implements DomainWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingDomainWatcher.class);

    @Override
    public void added(final Domain domain) {
        LOG.info("Domain {} added", domain);
    }

    @Override
    public void removed(final String domain) {
        LOG.info("Domain {} removed", domain);
    }
}
