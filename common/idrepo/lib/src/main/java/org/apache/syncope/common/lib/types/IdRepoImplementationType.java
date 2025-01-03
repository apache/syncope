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

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public final class IdRepoImplementationType {

    public static final String ACCOUNT_RULE = "ACCOUNT_RULE";

    public static final String PASSWORD_RULE = "PASSWORD_RULE";

    public static final String TASKJOB_DELEGATE = "TASKJOB_DELEGATE";

    public static final String REPORT_DELEGATE = "REPORT_DELEGATE";

    public static final String LOGIC_ACTIONS = "LOGIC_ACTIONS";

    public static final String MACRO_ACTIONS = "MACRO_ACTIONS";

    public static final String ATTR_VALUE_VALIDATOR = "ATTR_VALUE_VALIDATOR";

    public static final String DROPDOWN_VALUE_PROVIDER = "DROPDOWN_VALUE_PROVIDER";

    public static final String COMMAND = "COMMAND";

    public static final String RECIPIENTS_PROVIDER = "RECIPIENTS_PROVIDER";

    public static final String ITEM_TRANSFORMER = "ITEM_TRANSFORMER";

    private static final Map<String, String> VALUES = Map.ofEntries(
            Pair.of(ACCOUNT_RULE, "org.apache.syncope.core.provisioning.api.rules.AccountRule"),
            Pair.of(PASSWORD_RULE, "org.apache.syncope.core.provisioning.api.rules.PasswordRule"),
            Pair.of(TASKJOB_DELEGATE, "org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate"),
            Pair.of(REPORT_DELEGATE, "org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate"),
            Pair.of(LOGIC_ACTIONS, "org.apache.syncope.core.logic.api.LogicActions"),
            Pair.of(MACRO_ACTIONS, "org.apache.syncope.core.provisioning.api.macro.MacroActions"),
            Pair.of(ATTR_VALUE_VALIDATOR,
                    "org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValueValidator"),
            Pair.of(DROPDOWN_VALUE_PROVIDER,
                    "org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider"),
            Pair.of(COMMAND, "org.apache.syncope.core.provisioning.api.macro.Command"),
            Pair.of(RECIPIENTS_PROVIDER, "org.apache.syncope.core.provisioning.api.notification.RecipientsProvider"),
            Pair.of(ITEM_TRANSFORMER, "org.apache.syncope.core.provisioning.api.data.ItemTransformer"));

    public static Map<String, String> values() {
        return VALUES;
    }

    private IdRepoImplementationType() {
        // private constructor for static utility class
    }
}
