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
package org.apache.syncope.client.console.reports;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.types.ImplementationEngine;

public class ReportletWrapper implements Serializable {

    private static final long serialVersionUID = 2472755929742424558L;

    private final boolean isNew;

    private String implementationKey;

    private ImplementationEngine implementationEngine;

    private ReportletConf conf;

    private final Map<String, Pair<AbstractFiqlSearchConditionBuilder<?, ?, ?>, List<SearchClause>>> scondWrapper;

    public ReportletWrapper(final boolean isNew) {
        this.isNew = isNew;
        this.scondWrapper = new HashMap<>();
    }

    public boolean isNew() {
        return isNew;
    }

    public String getImplementationKey() {
        return implementationKey;
    }

    public ReportletWrapper setImplementationKey(final String implementationKey) {
        this.implementationKey = implementationKey;
        return this;
    }

    public ImplementationEngine getImplementationEngine() {
        return implementationEngine;
    }

    public ReportletWrapper setImplementationEngine(final ImplementationEngine implementationEngine) {
        this.implementationEngine = implementationEngine;
        return this;
    }

    public ReportletConf getConf() {
        return conf;
    }

    public ReportletWrapper setConf(final ReportletConf conf) {
        this.conf = conf;
        return this;
    }

    public Map<String, Pair<AbstractFiqlSearchConditionBuilder<?, ?, ?>, List<SearchClause>>> getSCondWrapper() {
        return scondWrapper;
    }
}
