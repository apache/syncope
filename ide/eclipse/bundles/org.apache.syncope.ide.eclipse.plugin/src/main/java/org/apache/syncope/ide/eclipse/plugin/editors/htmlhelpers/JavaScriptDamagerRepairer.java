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

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;

public class JavaScriptDamagerRepairer extends DefaultDamagerRepairer {

    public JavaScriptDamagerRepairer(final ITokenScanner scanner) {
        super(scanner);
    }

    public IRegion getDamageRegion(final ITypedRegion partition, final DocumentEvent e,
            final boolean documentPartitioningChanged) {
        if (!documentPartitioningChanged) {
            String source = fDocument.get();
            int start = source.substring(0, e.getOffset()).lastIndexOf("/*");
            if (start == -1) {
                start = 0;
            }
            int end = source.indexOf("*/", e.getOffset());
            int end2 = e.getOffset() + (e.getText() == null ? e.getLength() : e.getText().length());
            if (end == -1) {
                end = source.length();
            } else if (end2 > end) {
                end = end2;
            } else {
                end++;
            }

            return new Region(start, end - start);
        }
        return partition;
    }

}
