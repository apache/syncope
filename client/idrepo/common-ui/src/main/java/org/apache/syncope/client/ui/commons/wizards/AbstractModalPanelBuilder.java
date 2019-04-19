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
package org.apache.syncope.client.ui.commons.wizards;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.PageReference;
import org.apache.wicket.event.IEventSink;

public abstract class AbstractModalPanelBuilder<T extends Serializable> implements ModalPanelBuilder<T> {

    private static final long serialVersionUID = 5241745929825564456L;

    protected final PageReference pageRef;

    private final T defaultItem;

    private T item;

    protected IEventSink eventSink;

    /**
     * Construct.
     *
     * @param defaultItem default item.
     * @param pageRef Caller page reference.
     */
    public AbstractModalPanelBuilder(final T defaultItem, final PageReference pageRef) {
        this.defaultItem = defaultItem;
        this.pageRef = pageRef;
    }

    protected void onCancelInternal(final T modelObject) {
    }

    protected Serializable onApplyInternal(final T modelObject) {
        // do nothing
        return null;
    }

    protected T getOriginalItem() {
        return item;
    }

    @Override
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

    @Override
    public AbstractModalPanelBuilder<T> setItem(final T item) {
        this.item = item;
        return this;
    }

    @Override
    public PageReference getPageReference() {
        return pageRef;
    }

    @Override
    public ModalPanelBuilder<T> setEventSink(final IEventSink eventSink) {
        this.eventSink = eventSink;
        return this;
    }

    @Override
    public IEventSink getEventSink() {
        return eventSink;
    }
}
