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
package org.apache.syncope.client.ui.commons.markup.html.form;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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
import org.danekja.java.util.function.serializable.SerializableFunction;

public class AjaxPalettePanel<T extends Serializable> extends AbstractFieldPanel<List<T>> {

    private static final long serialVersionUID = 7738499668258805567L;

    protected Palette<T> palette;

    protected final Model<String> queryFilter = new Model<>(StringUtils.EMPTY);

    protected final List<T> availableBefore = new ArrayList<>();

    private final LoadableDetachableModel<List<T>> choicesModel;

    public AjaxPalettePanel(
            final String id, final IModel<List<T>> model, final Builder.Query<T> query, final Builder<T> builder) {

        super(id, builder.name == null ? id : builder.name, model);

        choicesModel = new PaletteLoadableDetachableModel(builder) {

            private static final long serialVersionUID = -108100712154481840L;

            @Override
            protected List<T> getChoices() {
                return query.execute(queryFilter.getObject());
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
                        ? getFilteredList(choices.getObject(), queryFilter.getObject().replaceAll("\\*", "\\.\\*"))
                        : choices.getObject();
            }
        };
        initialize(model, builder);
    }

    protected void initialize(final IModel<List<T>> model, final Builder<T> builder) {
        setOutputMarkupId(true);

        palette = buildPalette(model, builder);
        add(palette.setLabel(new ResourceModel(name)).setOutputMarkupId(true));

        Form<?> form = new Form<>("form");
        add(form.setEnabled(builder.filtered).setVisible(builder.filtered));

        queryFilter.setObject(builder.filter);
        AjaxTextFieldPanel filter = new AjaxTextFieldPanel("filter", "filter", queryFilter, false);
        form.add(filter.hideLabel().setOutputMarkupId(true));

        AjaxButton search = new AjaxButton("search") {

            private static final long serialVersionUID = 8390605330558248736L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                if (builder.warnIfEmptyFilter && StringUtils.isEmpty(queryFilter.getObject())) {
                    Session.get().info(getString("nomatch"));
                    ((BaseWebPage) getPage()).getNotificationPanel().refresh(target);
                }

                target.add(palette);
            }
        };
        search.setOutputMarkupId(true);
        form.add(search);
    }

    protected Palette<T> buildPalette(final IModel<List<T>> model, final Builder<T> builder) {
        return new NonI18nPalette<>(
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
                Recorder<T> recorder = new Recorder<>("recorder", this) {

                    private static final long serialVersionUID = -9169109967480083523L;

                    @Override
                    public List<T> getUnselectedList() {
                        IChoiceRenderer<? super T> renderer = getChoiceRenderer();
                        Collection<? extends T> choices = getChoices();

                        List<String> ids = builder.idExtractor.apply(getValue()).toList();
                        List<T> unselected = new ArrayList<>(choices.size());
                        choices.forEach(choice -> {
                            if (!ids.contains(renderer.getIdValue(choice, 0))) {
                                unselected.add(choice);
                            }
                        });

                        return unselected;
                    }

                    @Override
                    public List<T> getSelectedList() {
                        IChoiceRenderer<? super T> renderer = getChoiceRenderer();
                        Collection<? extends T> choices = getChoices();

                        // reduce number of method calls by building a lookup table
                        Map<T, String> idForChoice = choices.stream().collect(Collectors.toMap(
                                Function.identity(), choice -> renderer.getIdValue(choice, 0), (c1, c2) -> c1));

                        List<T> selected = new ArrayList<>(choices.size());
                        builder.idExtractor.apply(getValue()).forEach(id -> {
                            for (T choice : choices) {
                                if (id.equals(idForChoice.get(choice))) {
                                    selected.add(choice);
                                    break;
                                }
                            }
                        });

                        return selected;
                    }
                };
                recorder.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -6139318907146065915L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        processInput();
                        Optional.ofNullable(builder.event).ifPresent(e -> e.apply(target));
                    }
                });

                return recorder;
            }

            @Override
            protected Map<String, String> getAdditionalAttributes(final Object choice) {
                return builder.additionalAttributes == null
                        ? super.getAdditionalAttributes(choice)
                        : builder.additionalAttributes.apply(choice);
            }
        };
    }

    public Recorder<T> getRecorderComponent() {
        return palette.getRecorderComponent();
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

    @Override
    public AbstractFieldPanel<List<T>> setReadOnly(final boolean readOnly) {
        palette.setEnabled(!readOnly);
        return this;
    }

    @Override
    public AbstractFieldPanel<List<T>> setRequired(final boolean required) {
        palette.setRequired(required);
        return super.setRequired(required);
    }

    public static class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 991248996001040352L;

        protected String name;

        protected IChoiceRenderer<T> renderer = new SelectChoiceRenderer<>();

        protected boolean allowOrder;

        protected boolean allowMoveAll;

        protected String selectedLabel;

        protected String availableLabel;

        protected boolean filtered;

        protected String filter = "*";

        protected boolean warnIfEmptyFilter = true;

        protected SerializableFunction<String, Stream<String>> idExtractor =
                input -> Stream.of(Strings.split(input, ','));

        protected SerializableFunction<AjaxRequestTarget, Boolean> event;

        protected SerializableFunction<Object, Map<String, String>> additionalAttributes;

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

        public Builder<T> warnIfEmptyFilter(final boolean warnIfEmptyFilter) {
            this.warnIfEmptyFilter = warnIfEmptyFilter;
            return this;
        }

        public Builder<T> idExtractor(final SerializableFunction<String, Stream<String>> idExtractor) {
            this.idExtractor = idExtractor;
            return this;
        }

        public Builder<T> event(final SerializableFunction<AjaxRequestTarget, Boolean> event) {
            this.event = event;
            return this;
        }

        public Builder<T> additionalAttributes(
                final SerializableFunction<Object, Map<String, String>> additionalAttributes) {

            this.additionalAttributes = additionalAttributes;
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

            public abstract List<T> execute(String filter);
        }
    }

    protected abstract class PaletteLoadableDetachableModel extends LoadableDetachableModel<List<T>> {

        private static final long serialVersionUID = -7745220313769774616L;

        protected final Builder<T> builder;

        public PaletteLoadableDetachableModel(final Builder<T> builder) {
            this.builder = builder;
        }

        protected abstract List<T> getChoices();

        @Override
        protected List<T> load() {
            List<T> selected = availableBefore.isEmpty()
                    ? new ArrayList<>(palette.getModelCollection())
                    : getSelectedList(availableBefore);

            availableBefore.clear();
            availableBefore.addAll(ListUtils.sum(selected, getChoices()));
            return availableBefore;
        }

        protected List<T> getSelectedList(final Collection<T> choices) {
            IChoiceRenderer<? super T> renderer = palette.getChoiceRenderer();

            Map<T, String> idForChoice = choices.stream().collect(Collectors.toMap(
                    Function.identity(), choice -> renderer.getIdValue(choice, 0), (c1, c2) -> c1));

            List<T> selected = new ArrayList<>();
            builder.idExtractor.apply(palette.getRecorderComponent().getValue()).forEach(id -> {
                Iterator<T> iter = choices.iterator();
                boolean found = false;
                while (!found && iter.hasNext()) {
                    T choice = iter.next();
                    if (id.equals(idForChoice.get(choice))) {
                        selected.add(choice);
                        found = true;
                    }
                }
            });

            return selected;
        }

        protected List<T> getFilteredList(final Collection<T> choices, final String filter) {
            IChoiceRenderer<? super T> renderer = palette.getChoiceRenderer();

            Map<T, String> idForChoice = choices.stream().collect(Collectors.toMap(
                    Function.identity(), choice -> renderer.getIdValue(choice, 0), (c1, c2) -> c1));

            Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

            return choices.stream().
                    filter(choice -> pattern.matcher(idForChoice.get(choice)).matches()).
                    collect(Collectors.toList());
        }
    }

    public static class UpdateActionEvent {

        private final UserTO item;

        private final AjaxRequestTarget target;

        public UpdateActionEvent(final UserTO item, final AjaxRequestTarget target) {
            this.item = item;
            this.target = target;
        }

        public UserTO getItem() {
            return item;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
