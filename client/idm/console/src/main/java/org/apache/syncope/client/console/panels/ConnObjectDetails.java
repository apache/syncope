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
package org.apache.syncope.client.console.panels;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.wicket.model.Model;

public class ConnObjectDetails extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -6532127408741991806L;

    public ConnObjectDetails(final ConnObject connObjectTO) {
        super();

        MultilevelPanel mlp = new MultilevelPanel("details");
        mlp.setFirstLevel(new ConnObjectPanel(
                MultilevelPanel.FIRST_LEVEL_ID,
                Pair.of(Model.of(), Model.of()),
                Pair.of((ConnObject) null, connObjectTO),
                true));
        add(mlp);
    }

}
