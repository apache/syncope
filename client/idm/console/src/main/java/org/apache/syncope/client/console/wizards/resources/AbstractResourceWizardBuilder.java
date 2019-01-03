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
package org.apache.syncope.client.console.wizards.resources;

import java.io.Serializable;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal.ModalEvent;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Modal window with Resource form.
 *
 * @param <T> model object type
 */
public abstract class AbstractResourceWizardBuilder<T extends Serializable>
        extends BaseAjaxWizardBuilder<Serializable> {

    private static final long serialVersionUID = 1734415311027284221L;

    public AbstractResourceWizardBuilder(final T modelObject, final PageReference pageRef) {
        super(modelObject, pageRef);
    }

    public static class CreateEvent extends ModalEvent {

        private static final long serialVersionUID = -4488921035707289039L;

        private final String key;

        private final String displayName;

        private final Serializable parent;

        private final TopologyNode.Kind kind;

        public CreateEvent(
                final String key,
                final String displayName,
                final TopologyNode.Kind kind,
                final Serializable parent,
                final AjaxRequestTarget target) {
            super(target);
            this.key = key;
            this.displayName = displayName;
            this.kind = kind;
            this.parent = parent;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TopologyNode.Kind getKind() {
            return kind;
        }

        public Serializable getParent() {
            return parent;
        }
    }
}
