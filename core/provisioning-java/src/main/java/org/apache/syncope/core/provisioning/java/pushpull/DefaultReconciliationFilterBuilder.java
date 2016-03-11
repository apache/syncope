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
package org.apache.syncope.core.provisioning.java.pushpull;

import static org.identityconnectors.framework.impl.api.local.operations.FilteredResultsHandler.PassThroughFilter;

import org.identityconnectors.framework.common.objects.filter.Filter;
import org.apache.syncope.core.provisioning.api.pushpull.ReconciliationFilterBuilder;

/**
 * Default (pass-through) implementation of {@link ReconciliationFilterBuilder}.
 */
public abstract class DefaultReconciliationFilterBuilder implements ReconciliationFilterBuilder {

    private static final PassThroughFilter PASS_THROUGH = new PassThroughFilter();

    @Override
    public Filter build() {
        return PASS_THROUGH;
    }

}
