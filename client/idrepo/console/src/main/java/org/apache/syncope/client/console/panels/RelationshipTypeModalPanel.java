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

import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.wicket.PageReference;

public class RelationshipTypeModalPanel extends AbstractModalPanel<RelationshipTypeTO> {

    private static final long serialVersionUID = 1602285111803121341L;

    private final RelationshipTypeTO relationshipTypeTO;

    public RelationshipTypeModalPanel(
            final BaseModal<RelationshipTypeTO> modal,
            final RelationshipTypeTO relationshipTypeTO,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.relationshipTypeTO = relationshipTypeTO;
        add(new RelationshipTypeDetailsPanel("relationshipTypeDetails", this.relationshipTypeTO));
    }

    @Override
    public RelationshipTypeTO getItem() {
        return this.relationshipTypeTO;
    }
}
