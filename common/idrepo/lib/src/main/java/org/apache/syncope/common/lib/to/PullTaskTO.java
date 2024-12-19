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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.PullMode;

@Schema(allOf = { InboundTaskTO.class })
public class PullTaskTO extends InboundTaskTO implements TemplatableTO {

    private static final long serialVersionUID = -2143537546915809017L;

    @JsonProperty(required = true)
    private PullMode pullMode;

    private String reconFilterBuilder;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.PullTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    private final Map<String, AnyTO> templates = new HashMap<>();

    public PullMode getPullMode() {
        return pullMode;
    }

    public void setPullMode(final PullMode pullMode) {
        this.pullMode = pullMode;
    }

    public String getReconFilterBuilder() {
        return reconFilterBuilder;
    }

    public void setReconFilterBuilder(final String reconFilterBuilder) {
        this.reconFilterBuilder = reconFilterBuilder;
    }

    @Override
    public Map<String, AnyTO> getTemplates() {
        return templates;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(pullMode).
                append(reconFilterBuilder).
                append(templates).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PullTaskTO other = (PullTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(pullMode, other.pullMode).
                append(reconFilterBuilder, other.reconFilterBuilder).
                append(templates, other.templates).
                build();
    }
}
