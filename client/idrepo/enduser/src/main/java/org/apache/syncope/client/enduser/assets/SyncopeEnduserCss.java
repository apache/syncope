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
package org.apache.syncope.client.enduser.assets;

import de.agilecoders.wicket.core.Bootstrap;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;

public class SyncopeEnduserCss extends CssResourceReference {

    private static final long serialVersionUID = 7244898174745686253L;

    /**
     * Singleton instance of this reference.
     */
    public static final SyncopeEnduserCss INSTANCE = new SyncopeEnduserCss();

    public SyncopeEnduserCss() {
        super(SyncopeEnduserCss.class, "css/syncopeEnduser.css");
    }

    @Override
    public List<HeaderItem> getDependencies() {
        final List<HeaderItem> dependencies = new ArrayList<>();
        dependencies.add(CssHeaderItem.forReference(Bootstrap.getSettings().getCssResourceReference()));
        dependencies.addAll(super.getDependencies());
        return dependencies;
    }

}
