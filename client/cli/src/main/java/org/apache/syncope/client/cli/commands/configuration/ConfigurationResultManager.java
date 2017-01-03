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
package org.apache.syncope.client.cli.commands.configuration;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.client.cli.view.Table;
import org.apache.syncope.common.lib.to.AttrTO;

public class ConfigurationResultManager extends CommonsResultManager {

    public void fromGet(final List<AttrTO> attrTOs) {
        fromCommandToView("syncope configuration", attrTOs);
    }

    public void fromRead(final List<AttrTO> attrTOs) {
        fromCommandToView("selected conf attributes", attrTOs);
    }

    public void fromUpdate(final List<AttrTO> attrTOs) {
        fromCommandToView("updated conf attributes", attrTOs);
    }

    private void fromCommandToView(final String title, final List<AttrTO> attrTOs) {
        final Table.TableBuilder tableBuilder = new Table.TableBuilder(title).header("attribute").header("value");
        for (final AttrTO attrTO : attrTOs) {
            String attrValue = attrTO.getValues().toString();
            attrValue = attrValue.substring(0, attrValue.length() - 1);
            attrValue = attrValue.substring(1, attrValue.length());
            tableBuilder.rowValues(Arrays.asList(attrTO.getSchema(), attrValue));
        }
        tableBuilder.build().print();
    }
}
