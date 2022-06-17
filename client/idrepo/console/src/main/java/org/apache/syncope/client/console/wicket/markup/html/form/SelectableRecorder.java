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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

/**
 * A variant of Recorder, supporting single element selection (for editing purpose, for example). <b>Note</b>: this
 * class extends Recorder&lt;T&gt; but in fact it is a bare copy of most source code; this was done because the original
 * class is keeping everything private.
 *
 * @param <T> Type of the palette
 */
public class SelectableRecorder<T> extends Recorder<T> {

    private static final long serialVersionUID = -3009044376132921879L;

    private boolean attached = false;

    private static final String[] EMPTY_IDS = new String[0];

    /**
     * Conveniently maintained array of selected ids.
     */
    private String[] ids;

    private String selectedId;

    public SelectableRecorder(final String id, final Palette<T> palette) {
        super(id, palette);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (!getForm().hasError()) {
            initIds();
        } else if (ids == null) {
            ids = EMPTY_IDS;
        }
        attached = true;
    }

    /**
     * Synchronize ids collection from the palette's model
     */
    private void initIds() {
        // construct the model string based on selection collection
        IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();
        StringBuilder modelStringBuffer = new StringBuilder();
        Collection<T> modelCollection = getPalette().getModelCollection();
        if (modelCollection == null) {
            throw new WicketRuntimeException("Expected getPalette().getModelCollection() to return a non-null value."
                    + " Please make sure you have model object assigned to the palette");
        }
        Iterator<T> selection = modelCollection.iterator();

        int i = 0;
        while (selection.hasNext()) {
            modelStringBuffer.append(renderer.getIdValue(selection.next(), i++));
            if (selection.hasNext()) {
                modelStringBuffer.append(',');
            }
        }

        // set model and update ids array
        String modelString = modelStringBuffer.toString();
        setDefaultModel(new Model<>(modelString));
        updateIds(modelString);
    }

    public T getSelectedItem() {
        if (selectedId == null) {
            return null;
        }

        IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();

        T selected = null;
        for (T choice : getPalette().getChoices()) {
            if (renderer.getIdValue(choice, 0).equals(selectedId)) {
                selected = choice;
                break;
            }
        }

        return selected;
    }

    @Override
    public List<T> getSelectedList() {
        IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();
        if (ids.length == 0) {
            return List.of();
        }

        List<T> selected = new ArrayList<>(ids.length);
        for (String id : ids) {
            for (T choice : getPalette().getChoices()) {
                if (renderer.getIdValue(choice, 0).equals(id)) {
                    selected.add(choice);
                    break;
                }
            }
        }
        return selected;
    }

    @Override
    public List<T> getUnselectedList() {
        IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();
        Collection<? extends T> choices = getPalette().getChoices();

        if (choices.size() - ids.length == 0) {
            return List.of();
        }

        List<T> unselected = new ArrayList<>(Math.max(1, choices.size() - ids.length));
        for (T choice : choices) {
            final String choiceId = renderer.getIdValue(choice, 0);
            boolean selected = false;
            for (String id : ids) {
                if (id.equals(choiceId)) {
                    selected = true;
                    break;
                }
            }
            if (!selected) {
                unselected.add(choice);
            }
        }
        return unselected;
    }

    @Override
    protected void onValid() {
        super.onValid();
        if (attached) {
            updateIds();
        }
    }

    @Override
    protected void onInvalid() {
        super.onInvalid();
        if (attached) {
            updateIds();
        }
    }

    private void updateIds() {
        updateIds(getValue());
    }

    private void updateIds(final String value) {
        if (Strings.isEmpty(value)) {
            ids = EMPTY_IDS;
        } else if (value.indexOf('|') == -1) {
            ids = value.split(",");
            selectedId = null;
        } else {
            String[] splitted = value.split("\\|");
            selectedId = splitted[0];
            ids = splitted[1].split(",");
        }
    }
}
