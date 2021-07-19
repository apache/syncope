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
package org.apache.syncope.client.ui.commons;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;

/**
 * Implements (custom) Domain DropDownChoice component.
 */
public class DomainDropDown extends DropDownChoice<String> {

    private static final long serialVersionUID = -7401167913360133325L;

    public DomainDropDown(final String id, final IModel<List<String>> domains) {
        super(id, domains);
        setModel(new IModel<>() {

            private static final long serialVersionUID = -1124206668056084806L;

            @Override
            public String getObject() {
                return BaseSession.class.cast(Session.get()).getDomain();
            }

            @Override
            public void setObject(final String object) {
                BaseSession.class.cast(Session.get()).setDomain(object);
            }

            @Override
            public void detach() {
                // Empty.
            }
        });
        // set default value to Master Domain
        getModel().setObject(SyncopeConstants.MASTER_DOMAIN);

        setOutputMarkupPlaceholderTag(true);
        if (domains.getObject().size() == 1) {
            setVisible(false);
        }
    }
}
