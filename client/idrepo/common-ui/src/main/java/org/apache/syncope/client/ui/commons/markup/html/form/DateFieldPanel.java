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

import com.googlecode.wicket.kendo.ui.resource.KendoCultureResourceReference;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.util.StringUtils;

public abstract class DateFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = -428975732068281726L;

    protected final FastDateFormat fmt;

    protected DateFieldPanel(final String id, final String name, final IModel<Date> model, final FastDateFormat fmt) {
        super(id, name, model);
        this.fmt = fmt;
    }

    @Override
    public FieldPanel<Date> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public Date getObject() {
                Date date = null;
                if (list != null && !list.isEmpty() && StringUtils.hasText(list.getFirst().toString())) {
                    try {
                        // Parse string using datePattern
                        date = fmt.parse(list.getFirst().toString());
                    } catch (ParseException e) {
                        LOG.error("invalid parse exception", e);
                    }
                }

                return date;
            }

            @Override
            public void setObject(final Date object) {
                list.clear();
                if (object != null) {
                    list.add(fmt.format(object));
                }
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FieldPanel<Date> setNewModel(final ListItem item) {
        IModel<Date> model = new Model<>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Date getObject() {
                Date date = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        // Parse string using datePattern
                        try {
                            date = fmt.parse(obj.toString());
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                    } else if (obj instanceof final Date date1) {
                        // Don't parse anything
                        date = date1;
                    } else {
                        // consider Long
                        date = new Date((Long) obj);
                    }
                }

                return date;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final Date object) {
                item.setModelObject(Optional.ofNullable(object).map(fmt::format).orElse(null));
            }
        };

        field.setModel(model);
        return this;
    }

    @Override
    public FieldPanel<Date> setNewModel(final Attributable attributable, final String schema) {
        field.setModel(new Model<>() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Date getObject() {
                return attributable.getPlainAttr(schema).map(Attr::getValues).filter(Predicate.not(List::isEmpty)).
                        map(values -> {
                            try {
                                return fmt.parse(values.getFirst());
                            } catch (ParseException e) {
                                LOG.error("While parsing date", e);
                                return null;
                            }
                        }).orElse(null);
            }

            @Override
            public void setObject(final Date object) {
                attributable.getPlainAttr(schema).ifPresent(plainAttr -> {
                    plainAttr.getValues().clear();
                    Optional.ofNullable(object).ifPresent(o -> plainAttr.getValues().add(fmt.format(object)));
                });
            }
        });

        return this;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(new KendoCultureResourceReference(getLocale())));
    }
}
