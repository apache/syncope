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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class HTMLFileDocumentProvider extends FileDocumentProvider {

    public IDocument createDocument(final Object element) throws CoreException {
        IDocument document = super.createDocument(element);
        if (document != null) {
            IDocumentPartitioner partitioner =
                new FastPartitioner(
                        new HTMLPartitionScanner(),
                        new String[]{
                                HTMLPartitionScanner.HTML_TAG,
                                HTMLPartitionScanner.HTML_COMMENT,
                                HTMLPartitionScanner.HTML_SCRIPT,
                                HTMLPartitionScanner.HTML_DOCTYPE,
                                HTMLPartitionScanner.HTML_DIRECTIVE,
                                HTMLPartitionScanner.JAVASCRIPT,
                                HTMLPartitionScanner.HTML_CSS,
                                HTMLPartitionScanner.SYNCOPE_TAG});
            partitioner.connect(document);
            document.setDocumentPartitioner(partitioner);
        }
        return document;
    }

}
