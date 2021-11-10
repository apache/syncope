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
package org.apache.syncope.core.logic.cocoon;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.apache.cocoon.pipeline.caching.CacheKey;
import org.apache.cocoon.pipeline.caching.SimpleCacheKey;
import org.apache.cocoon.pipeline.component.CachingPipelineComponent;
import org.apache.cocoon.sax.AbstractSAXSerializer;
import org.apache.tika.sax.ToTextContentHandler;

public class TextSerializer extends AbstractSAXSerializer implements CachingPipelineComponent {

    @Override
    public void setOutputStream(final OutputStream outputStream) {
        super.setOutputStream(outputStream);
        setContentHandler(new ToTextContentHandler(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
    }

    @Override
    public CacheKey constructCacheKey() {
        return new SimpleCacheKey();
    }
}
