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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.cocoon.pipeline.SetupException;
import org.apache.cocoon.pipeline.caching.CacheKey;
import org.apache.cocoon.pipeline.component.CachingPipelineComponent;
import org.apache.cocoon.pipeline.util.StringRepresentation;
import org.apache.cocoon.sax.AbstractSAXTransformer;
import org.apache.cocoon.sax.SAXConsumer;
import org.apache.cocoon.sax.util.SAXConsumerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSLTTransformer extends AbstractSAXTransformer implements CachingPipelineComponent {

    private static final Logger LOG = LoggerFactory.getLogger(XSLTTransformer.class);

    /**
     * A generic transformer factory to parse XSLTs.
     */
    private static final SAXTransformerFactory TRAX_FACTORY = createNewSAXTransformerFactory();

    /**
     * The XSLT parameters name pattern.
     */
    private static final Pattern XSLT_PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][\\w\\-\\.]*");

    /**
     * The XSLT parameters reference.
     */
    private Map<String, Object> parameters;

    /**
     * The XSLT Template reference.
     */
    private Templates templates;

    private Source source;

    public XSLTTransformer(final Source source) {
        super();
        this.load(source, null);
    }

    /**
     * Creates a new transformer reading the XSLT from the Source source and setting the TransformerFactory attributes.
     *
     * This constructor is useful when users want to perform XSLT transformation using <a
     * href="http://xml.apache.org/xalan-j/xsltc_usage.html">xsltc</a>.
     *
     * @param source the XSLT source
     * @param attributes the Transformer Factory attributes
     */
    public XSLTTransformer(final Source source, final Map<String, Object> attributes) {
        super();
        this.load(source, attributes);
    }

    /**
     * Method useful to create a new transformer reading the XSLT from the URL source and setting the Transformer
     * Factory attributes.
     *
     * This method is useful when users want to perform XSLT transformation using <a
     * href="http://xml.apache.org/xalan-j/xsltc_usage.html">xsltc</a>.
     *
     * @param source the XSLT source
     * @param attributes the Transformer Factory attributes
     */
    private void load(final Source source, final Map<String, Object> attributes) {
        if (source == null) {
            throw new IllegalArgumentException("The parameter 'source' mustn't be null.");
        }

        this.source = source;

        this.load(this.source, this.source.toString(), attributes);
    }

    private void load(final Source source, final String localCacheKey, final Map<String, Object> attributes) {
        LOG.debug("{} local cache miss: {}", getClass().getSimpleName(), localCacheKey);

        // XSLT has to be parsed
        final SAXTransformerFactory transformerFactory;
        if (attributes == null || attributes.isEmpty()) {
            transformerFactory = TRAX_FACTORY;
        } else {
            transformerFactory = createNewSAXTransformerFactory();
            attributes.forEach(transformerFactory::setAttribute);
        }

        try {
            this.templates = transformerFactory.newTemplates(source);
        } catch (TransformerConfigurationException e) {
            throw new SetupException("Impossible to read XSLT from '" + source + "', see nested exception", e);
        }
    }

    /**
     * Sets the XSLT parameters to be applied to XSLT stylesheet.
     *
     * @param parameters the XSLT parameters to be applied to XSLT stylesheet
     */
    public void setParameters(final Map<String, ? extends Object> parameters) {
        if (parameters == null) {
            this.parameters = null;
        } else {
            this.parameters = new HashMap<>(parameters);
        }
    }

    @Override
    protected void setSAXConsumer(final SAXConsumer consumer) {
        TransformerHandler transformerHandler;
        try {
            transformerHandler = TRAX_FACTORY.newTransformerHandler(this.templates);
        } catch (Exception e) {
            throw new SetupException("Could not initialize transformer handler.", e);
        }

        if (this.parameters != null) {
            final Transformer transformer = transformerHandler.getTransformer();

            this.parameters.forEach((name, values) -> {
                // is valid XSLT parameter name
                if (XSLT_PARAMETER_NAME_PATTERN.matcher(name).matches()) {
                    transformer.setParameter(name, values);
                }
            });
        }

        final SAXResult result = new SAXResult();
        result.setHandler(consumer);
        // According to TrAX specs, all TransformerHandlers are LexicalHandlers
        result.setLexicalHandler(consumer);
        transformerHandler.setResult(result);

        final SAXConsumerAdapter saxConsumerAdapter = new SAXConsumerAdapter();
        saxConsumerAdapter.setContentHandler(transformerHandler);
        super.setSAXConsumer(saxConsumerAdapter);
    }

    @Override
    public CacheKey constructCacheKey() {
        return null;
    }

    /**
     * Utility method to create a new transformer factory.
     *
     * @return a new transformer factory
     */
    private static SAXTransformerFactory createNewSAXTransformerFactory() {
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transformerFactory.setURIResolver((href, base) -> null);
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            LOG.error("Could not enable secure XML processing", e);
        }
        return transformerFactory;
    }

    @Override
    public String toString() {
        return StringRepresentation.buildString(this, "src=<" + this.source + '>');
    }
}
