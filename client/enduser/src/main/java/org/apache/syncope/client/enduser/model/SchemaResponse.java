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
package org.apache.syncope.client.enduser.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;

public class SchemaResponse implements Serializable {

    private static final long serialVersionUID = -8896862106241712829L;

    private List<AbstractSchemaTO> plainSchemas = new ArrayList<>();

    private List<AbstractSchemaTO> derSchemas = new ArrayList<>();

    private List<AbstractSchemaTO> virSchemas = new ArrayList<>();

    public SchemaResponse() {
    }

    public List<AbstractSchemaTO> getPlainSchemas() {
        return plainSchemas;
    }

    public void setPlainSchemas(final List<AbstractSchemaTO> plainSchemas) {
        this.plainSchemas = plainSchemas;
    }

    public List<AbstractSchemaTO> getDerSchemas() {
        return derSchemas;
    }

    public void setDerSchemas(final List<AbstractSchemaTO> derSchemas) {
        this.derSchemas = derSchemas;
    }

    public List<AbstractSchemaTO> getVirSchemas() {
        return virSchemas;
    }

    public void setVirSchemas(final List<AbstractSchemaTO> virSchemas) {
        this.virSchemas = virSchemas;
    }

    public SchemaResponse plainSchemas(final List<AbstractSchemaTO> value) {
        this.plainSchemas = value;
        return this;
    }

    public SchemaResponse derSchemas(final List<AbstractSchemaTO> value) {
        this.derSchemas = value;
        return this;
    }

    public SchemaResponse virSchemas(final List<AbstractSchemaTO> value) {
        this.virSchemas = value;
        return this;
    }
}
