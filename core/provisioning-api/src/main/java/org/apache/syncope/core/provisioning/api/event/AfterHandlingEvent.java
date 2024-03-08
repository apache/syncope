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
package org.apache.syncope.core.provisioning.api.event;

import java.io.Serializable;
import org.apache.syncope.common.lib.types.OpEvent;

@SuppressWarnings({ "squid:S00107", "squid:S1948" })
public class AfterHandlingEvent implements Serializable {

    private static final long serialVersionUID = 5950986229089263378L;

    public static final String JOBMAP_KEY = "AfterHandlingEvent";

    private final String domain;

    private final String who;

    private final OpEvent.CategoryType type;

    private final String category;

    private final String subcategory;

    private final String op;

    private final OpEvent.Outcome outcome;

    private final Object before;

    private final Object output;

    private final Object[] input;

    public AfterHandlingEvent(
            final String domain,
            final String who,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final Object before,
            final Object output,
            final Object... input) {

        this.domain = domain;
        this.who = who;
        this.type = type;
        this.category = category;
        this.subcategory = subcategory;
        this.op = op;
        this.outcome = outcome;
        this.before = before;
        this.output = output;
        this.input = input;
    }

    public String getDomain() {
        return domain;
    }

    public String getWho() {
        return who;
    }

    public OpEvent.CategoryType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getOp() {
        return op;
    }

    public OpEvent.Outcome getOutcome() {
        return outcome;
    }

    public Object getBefore() {
        return before;
    }

    public Object getOutput() {
        return output;
    }

    public Object[] getInput() {
        return input;
    }
}
