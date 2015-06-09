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
package org.apache.syncope.client.console.topology;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;

public class TopologyReloadBehavior extends Behavior {

    private static final long serialVersionUID = 1L;

    private final String source;

    private final String target;

    private final TopologyNode.Status status;

    public TopologyReloadBehavior(final String source, final String target, final TopologyNode.Status status) {
        this.source = source;
        this.target = target;
        this.status = status;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {

        switch (status) {
            case UNKNOWN:
                response.render(OnLoadHeaderItem.forScript(String.format("unknown('%s', '%s')", source, target)));
                break;
            case REACHABLE:
                response.render(OnLoadHeaderItem.forScript(String.format("enable('%s', '%s')", source, target)));
                break;
            case UNREACHABLE:
                response.render(OnLoadHeaderItem.forScript(String.format("disable('%s', '%s')", source, target)));
                break;
            default:
                response.render(OnLoadHeaderItem.forScript(String.format("failure('%s', '%s')", source, target)));
            // remove subtree
        }
    }

}
