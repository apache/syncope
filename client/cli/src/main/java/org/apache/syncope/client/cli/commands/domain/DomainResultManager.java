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
package org.apache.syncope.client.cli.commands.domain;

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.DomainTO;

public class DomainResultManager extends CommonsResultManager {

    public void printDomains(final List<DomainTO> domainTOs) {
        System.out.println("");
        for (final DomainTO domainTO : domainTOs) {
            printDomain(domainTO);
        }
    }

    public void printDomain(final DomainTO domainTO) {
        System.out.println(" > DOIMAIN KEY: " + domainTO.getKey());
        System.out.println("    cipher algorithm: " + domainTO.getAdminCipherAlgorithm());
        System.out.println("");
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("domains details", details);
    }
}
