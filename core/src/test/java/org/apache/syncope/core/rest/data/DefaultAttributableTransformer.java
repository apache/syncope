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
package org.apache.syncope.core.rest.data;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;

/**
 * Class for integration tests: transform (by making it double) any attribute value for defined schema.
 */
public class DefaultAttributableTransformer implements AttributableTransformer {

    private static final String NAME = "makeItDouble";

    @Override
    public <T extends AbstractAttributableTO> T transform(final T input) {
        for (AttributeTO attr : input.getAttributes()) {
            if (NAME.equals(attr.getSchema())) {
                List<String> values = new ArrayList<String>(attr.getValues().size());
                for (String value : attr.getValues()) {
                    values.add(String.valueOf(2 * Long.valueOf(value)));
                }
                attr.getValues().clear();
                attr.getValues().addAll(values);
            }
        }

        return input;
    }

    @Override
    public <T extends AbstractAttributableMod> T transform(final T input) {
        for (AttributeMod attr : input.getAttributesToBeUpdated()) {
            if (NAME.equals(attr.getSchema())) {
                List<String> values = new ArrayList<String>(attr.getValuesToBeAdded().size());
                for (String value : attr.getValuesToBeAdded()) {
                    values.add(String.valueOf(2 * Long.valueOf(value)));
                }
                attr.getValuesToBeAdded().clear();
                attr.getValuesToBeAdded().addAll(values);
            }
        }

        return input;
    }
}
