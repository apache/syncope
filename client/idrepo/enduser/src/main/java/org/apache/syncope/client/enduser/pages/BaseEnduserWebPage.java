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
package org.apache.syncope.client.enduser.pages;

import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.navigation.Navbar;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class BaseEnduserWebPage extends BaseWebPage {

    private static final long serialVersionUID = 5760583420031293480L;

    protected final Navbar navbar;

    @SpringBean
    protected ClassPathScanImplementationLookup lookup;

    public BaseEnduserWebPage() {
        this(null);

        body.add(navbar);
    }

    public BaseEnduserWebPage(final PageParameters parameters) {
        super(parameters);

        navbar = new Navbar("navbar", lookup.getExtPageClasses());
        body.add(navbar);
    }
}
