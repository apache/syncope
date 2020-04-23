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

import java.io.File;
import java.io.OutputStream;

import org.apache.cocoon.pipeline.ProcessingException;
import org.apache.cocoon.pipeline.caching.CacheKey;
import org.apache.cocoon.pipeline.caching.SimpleCacheKey;
import org.apache.cocoon.pipeline.component.CachingPipelineComponent;
import org.apache.cocoon.pipeline.util.StringRepresentation;
import org.apache.cocoon.sax.AbstractSAXSerializer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.xmlgraphics.util.MimeConstants;
import org.xml.sax.ContentHandler;

public class FopSerializer extends AbstractSAXSerializer implements CachingPipelineComponent {

    private static final FopFactory FOP_FACTORY = new FopFactoryBuilder(new File(".").toURI()).build();

    private String outputFormat;

    /**
     * Create a new FOP serializer that produces a PDF in output
     */
    public FopSerializer() {
        this(MimeConstants.MIME_PDF);
    }

    /**
     * Create a new FOP serializer that produces the specified mime
     *
     * @param outputFormat the output's mime type
     */
    public FopSerializer(final String outputFormat) {
        if (outputFormat == null) {
            throw new IllegalArgumentException("The parameter 'outputFormat' mustn't be null.");
        }

        this.outputFormat = outputFormat;
    }

    @Override
    public CacheKey constructCacheKey() {
        return new SimpleCacheKey();
    }

    @Override
    public void setOutputStream(final OutputStream outputStream) {
        try {
            Fop fop = FOP_FACTORY.newFop(this.outputFormat, outputStream);
            ContentHandler fopContentHandler = fop.getDefaultHandler();

            this.setContentHandler(fopContentHandler);
        } catch (FOPException e) {
            throw new ProcessingException("Impossible to initialize FOPSerializer", e);
        }
    }

    @Override
    public String toString() {
        return StringRepresentation.buildString(this, "outputFormat=" + this.outputFormat);
    }
}
