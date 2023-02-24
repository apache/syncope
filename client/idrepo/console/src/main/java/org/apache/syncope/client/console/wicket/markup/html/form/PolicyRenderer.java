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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.util.Map;
import org.apache.wicket.markup.html.form.ChoiceRenderer;

public class PolicyRenderer extends ChoiceRenderer<String> {

    private static final long serialVersionUID = 8060500161321947000L;

    private final Map<String, String> policies;

    public PolicyRenderer(final Map<String, String> policies) {
        super();
        this.policies = policies;
    }

    @Override
    public Object getDisplayValue(final String object) {
        return policies.get(object);
    }

    @Override
    public String getIdValue(final String object, final int index) {
        return String.valueOf(object != null ? object : 0L);
    }
}
