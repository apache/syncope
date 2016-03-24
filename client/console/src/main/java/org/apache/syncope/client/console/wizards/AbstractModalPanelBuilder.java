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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.wicket.PageReference;

public abstract class AbstractModalPanelBuilder<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 5241745929825564456L;

    protected final String id;

    protected final PageReference pageRef;

    private final T defaultItem;

    private T item;

    /**
     * Construct.
     *
     * @param id The component id
     * @param defaultItem default item.
     * @param pageRef Caller page reference.
     */
    public AbstractModalPanelBuilder(final String id, final T defaultItem, final PageReference pageRef) {
        this.id = id;
        this.defaultItem = defaultItem;
        this.pageRef = pageRef;
    }

    public abstract ModalPanel<T> build(final int index, final AjaxWizard.Mode mode);

    protected void onCancelInternal(final T modelObject) {
    }

    protected Serializable onApplyInternal(final T modelObject) {
        // do nothing
        return null;
    }

    protected T getOriginalItem() {
        return item;
    }

    public T getDefaultItem() {
        return defaultItem;
    }

    protected T newModelObject() {
        if (item == null) {
            // keep the original item: the which one before the changes performed during wizard browsing
            item = SerializationUtils.clone(defaultItem);
        }

        // instantiate a new model object and return it
        return SerializationUtils.clone(item);
    }

    /**
     * Replaces the default value provided with the constructor and nullify working item object.
     *
     * @param item new value.
     * @return the current wizard factory instance.
     */
    public AbstractModalPanelBuilder<T> setItem(final T item) {
        this.item = item;
        return this;
    }

    public PageReference getPageReference() {
        return pageRef;
    }
}
