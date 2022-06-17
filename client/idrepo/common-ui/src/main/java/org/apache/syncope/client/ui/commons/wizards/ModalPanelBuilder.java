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
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.event.IEventSink;

public interface ModalPanelBuilder<T extends Serializable> extends Serializable {

    /**
     * Build the wizard.
     *
     * @param id component id.
     * @param index step index.
     * @param mode mode.
     * @return wizard.
     */
    WizardModalPanel<T> build(String id, int index, AjaxWizard.Mode mode);

    T getDefaultItem();

    PageReference getPageReference();

    /**
     * Replaces the default value provided with the constructor and nullify working item object.
     *
     * @param item new value.
     * @return the current wizard factory instance.
     */
    ModalPanelBuilder<T> setItem(T item);

    ModalPanelBuilder<T> setEventSink(IEventSink eventSink);

    IEventSink getEventSink();
}
