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
package org.apache.syncope.fit.core.reference;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.core.provisioning.api.LogicActions;

/**
 * Class for integration tests: transform (by making it double) any attribute value for defined schema.
 */
public class DoubleValueLogicActions implements LogicActions {

    private static final String NAME = "makeItDouble";

    @Override
    public <A extends AnyTO> A beforeCreate(final A input) {
        for (AttrTO attr : input.getPlainAttrs()) {
            if (NAME.equals(attr.getSchema())) {
                List<String> values = new ArrayList<>(attr.getValues().size());
                for (String value : attr.getValues()) {
                    try {
                        values.add(String.valueOf(2 * Long.parseLong(value)));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                attr.getValues().clear();
                attr.getValues().addAll(values);
            }
        }

        return input;
    }

    @Override
    public <M extends AnyPatch> M beforeUpdate(final M input) {
        for (AttrPatch patch : input.getPlainAttrs()) {
            if (NAME.equals(patch.getAttrTO().getSchema())) {
                List<String> values = new ArrayList<>(patch.getAttrTO().getValues().size());
                for (String value : patch.getAttrTO().getValues()) {
                    try {
                        values.add(String.valueOf(2 * Long.parseLong(value)));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                patch.getAttrTO().getValues().clear();
                patch.getAttrTO().getValues().addAll(values);
            }
        }

        return input;
    }
}
