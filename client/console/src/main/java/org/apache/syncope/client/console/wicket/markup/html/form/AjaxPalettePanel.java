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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;

public class AjaxPalettePanel<T extends Serializable> extends AbstractFieldPanel<List<T>> {

    private static final long serialVersionUID = 7738499668258805567L;

    protected Palette<T> palette;

    private final Model<String> queryFilter = new Model<>(StringUtils.EMPTY);

    private final List<T> availableBefore = new ArrayList<>();

    private final LoadableDetachableModel<List<T>> choicesModel;

    public AjaxPalettePanel(
            final String id, final IModel<List<T>> model, final Builder.Query<T> choices, final Builder<T> builder) {
        super(id, builder.name == null ? id : builder.name, model);

        choicesModel = new PaletteLoadableDetachableModel(builder) {

            private static final long serialVersionUID = -108100712154481840L;

            @Override
            protected List<T> getChoices() {
                return choices.execute(getFilter());
            }
        };
        initialize(model, builder);
    }

    public AjaxPalettePanel(
            final String id, final IModel<List<T>> model, final IModel<List<T>> choices, final Builder<T> builder) {
        super(id, builder.name == null ? id : builder.name, model);

        choicesModel = new PaletteLoadableDetachableModel(builder) {

            private static final long serialVersionUID = -108100712154481840L;

            @Override
            protected List<T> getChoices() {
                return builder.filtered
                        ? getFilteredList(choices.getObject(), getFilter().replaceAll("\\*", "\\.\\*"))
                        : choices.getObject();
            }
        };
        initialize(model, builder);
    }

    private void initialize(final IModel<List<T>> model, final Builder<T> builder) {
        setOutputMarkupId(true);

        this.palette = new NonI18nPalette<T>(
                "paletteField", model, choicesModel, builder.renderer, 8, builder.allowOrder, builder.allowMoveAll) {

            private static final long serialVersionUID = -3074655279011678437L;

            @Override
            protected Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("palette.available", builder.availableLabel));
            }

            @Override
            protected Component newSelectedHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("palette.selected", builder.selectedLabel));
            }

            @Override
            protected Recorder<T> newRecorderComponent() {
                return new Recorder<T>("recorder", this) {

                    private static final long serialVersionUID = -9169109967480083523L;

                    @Override
                    public List<T> getUnselectedList() {
                        final IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();
                        final Collection<? extends T> choices = getPalette().getChoices();
                        final List<T> unselected = new ArrayList<>(choices.size());
                        final List<String> ids = Arrays.asList(getValue().split(","));

                        for (final T choice : choices) {
                            final String choiceId = renderer.getIdValue(choice, 0);

                            if (!ids.contains(choiceId)) {
                                unselected.add(choice);
                            }
                        }

                        return unselected;
                    }

                    @Override
                    public List<T> getSelectedList() {
                        final IChoiceRenderer<? super T> renderer = getPalette().getChoiceRenderer();
                        final Collection<? extends T> choices = getPalette().getChoices();
                        final List<T> selected = new ArrayList<>(choices.size());

                        // reduce number of method calls by building a lookup table
                        final Map<T, String> idForChoice = new HashMap<>(choices.size());
                        for (final T choice : choices) {
                            idForChoice.put(choice, renderer.getIdValue(choice, 0));
                        }

                        final String value = getValue();
                        int start = value.indexOf(';') + 1;

                        for (final String id : Strings.split(value.substring(start), ',')) {
                            for (final T choice : choices) {
                                final String idValue = idForChoice.get(choice);
                                if (id.equals(idValue)) {
                                    selected.add(choice);
                                    break;
                                }
                            }
                        }

                        return selected;
                    }
                };
            }
        };

        add(palette.setOutputMarkupId(true));

        final Form<?> form = new Form<>("form");
        add(form.setEnabled(builder.filtered).setVisible(builder.filtered));

        final AjaxTextFieldPanel filter = new AjaxTextFieldPanel("filter", "filter", queryFilter, false);
        filter.hideLabel().setOutputMarkupId(true);
        form.add(filter);

        form.add(new AjaxSubmitLink("search") {

            private static final long serialVersionUID = -1765773642975892072L;

            @Override
            protected void onAfterSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onAfterSubmit(target, form);
                target.add(palette);
            }
        });
    }

    public LoadableDetachableModel<List<T>> getChoicesModel() {
        return choicesModel;
    }

    @Override
    public AjaxPalettePanel<T> setModelObject(final List<T> object) {
        palette.setDefaultModelObject(object);
        return this;
    }

    public Collection<T> getModelCollection() {
        return palette.getModelCollection();
    }

    public void reload(final AjaxRequestTarget target) {
        target.add(palette);
    }

    public static class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 991248996001040352L;

        private IChoiceRenderer<T> renderer;

        private boolean allowOrder;

        private boolean allowMoveAll;

        private String selectedLabel;

        private String availableLabel;

        private boolean filtered;

        private final AjaxPaletteConf conf = new AjaxPaletteConf();

        private String filter = conf.getDefaultFilter();

        private String name;

        public Builder() {
            this.allowMoveAll = false;
            this.allowOrder = false;
            this.filtered = false;
            this.renderer = new SelectChoiceRenderer<>();
        }

        public Builder<T> setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder<T> setAllowOrder(final boolean allowOrder) {
            this.allowOrder = allowOrder;
            return this;
        }

        public Builder<T> setAllowMoveAll(final boolean allowMoveAll) {
            this.allowMoveAll = allowMoveAll;
            return this;
        }

        public Builder<T> setSelectedLabel(final String selectedLabel) {
            this.selectedLabel = selectedLabel;
            return this;
        }

        public Builder<T> setAvailableLabel(final String availableLabel) {
            this.availableLabel = availableLabel;
            return this;
        }

        public Builder<T> setRenderer(final IChoiceRenderer<T> renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder<T> withFilter() {
            this.filtered = true;
            return this;
        }

        public Builder<T> withFilter(final String defaultFilter) {
            this.filtered = true;
            this.filter = defaultFilter;
            return this;
        }

        public AjaxPalettePanel<T> build(final String id, final IModel<List<T>> model, final IModel<List<T>> choices) {
            return new AjaxPalettePanel<>(id, model, choices, this);
        }

        public AjaxPalettePanel<T> build(final String id, final IModel<List<T>> model, final Query<T> choices) {
            return new AjaxPalettePanel<>(id, model, choices, this);
        }

        public abstract static class Query<T extends Serializable> implements Serializable {

            private static final long serialVersionUID = 3582312993557742858L;

            public abstract List<T> execute(final String filter);
        }
    }

    private abstract class PaletteLoadableDetachableModel extends LoadableDetachableModel<List<T>> {

        private static final long serialVersionUID = -7745220313769774616L;

        private final Builder<T> builder;

        PaletteLoadableDetachableModel(final Builder<T> builder) {
            super();
            this.builder = builder;
        }

        protected abstract List<T> getChoices();

        protected String getFilter() {
            return StringUtils.isBlank(queryFilter.getObject()) ? builder.filter : queryFilter.getObject();
        }

        @Override
        protected List<T> load() {
            final List<T> selected = availableBefore.isEmpty()
                    ? new ArrayList<>(palette.getModelCollection())
                    : getSelectedList(availableBefore, palette.getRecorderComponent().getValue());

            availableBefore.clear();
            availableBefore.addAll(ListUtils.sum(selected, getChoices()));
            return availableBefore;
        }

        private List<T> getSelectedList(final Collection<T> choices, final String selection) {
            final IChoiceRenderer<? super T> renderer = palette.getChoiceRenderer();
            final List<T> selected = new ArrayList<>();

            final Map<T, String> idForChoice = new HashMap<>();
            for (final T choice : choices) {
                idForChoice.put(choice, renderer.getIdValue(choice, 0));
            }

            for (final String id : Strings.split(selection, ',')) {
                final Iterator<T> iter = choices.iterator();
                boolean found = false;
                while (!found && iter.hasNext()) {
                    final T choice = iter.next();
                    final String idValue = idForChoice.get(choice);
                    if (id.equals(idValue)) {
                        selected.add(choice);
                        found = true;
                    }
                }
            }

            return selected;
        }

        protected List<T> getFilteredList(final Collection<T> choices, final String filter) {
            final IChoiceRenderer<? super T> renderer = palette.getChoiceRenderer();
            final List<T> selected = new ArrayList<>(choices.size());

            final Map<T, String> idForChoice = new HashMap<>();
            for (final T choice : choices) {
                idForChoice.put(choice, renderer.getIdValue(choice, 0));
            }

            final Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

            for (T choice : choices) {
                final String idValue = idForChoice.get(choice);
                if (pattern.matcher(idValue).matches()) {
                    selected.add(choice);
                }
            }

            return selected;
        }
    }
}
