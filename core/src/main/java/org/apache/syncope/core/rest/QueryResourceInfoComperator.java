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
package org.apache.syncope.core.rest;

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoComparator;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class QueryResourceInfoComperator extends OperationResourceInfoComparator implements ResourceComparator {

    public QueryResourceInfoComperator() {
        super(null, null);
    }

    @Override
    public int compare(final ClassResourceInfo cri1, final ClassResourceInfo cri2, final Message message) {
        // Leave Class selection to CXF
        return 0;
    }

    @Override
    public int compare(final OperationResourceInfo oper1, final OperationResourceInfo oper2, final Message message) {

        // Check if CXF can make a decision
        int cxfResult = super.compare(oper1, oper2);
        if (cxfResult != 0) {
            return cxfResult;
        }

        int op1Counter = getMatchingRate(oper1, message);
        int op2Counter = getMatchingRate(oper2, message);

        return op1Counter == op2Counter
                ? 0
                : op1Counter < op2Counter
                        ? 1
                        : -1;
    }

    /**
     * This method calculates a number indicating a good or bad match between values provided within the request and
     * expected method parameters. A higher number means a better match.
     *
     * @param operation The operation to be rated, based on contained parameterInfo values.
     * @param message A message containing query and header values from user request
     * @return A positive or negative number, indicating a good match between query and method
     */
    protected int getMatchingRate(final OperationResourceInfo operation, final Message message) {

        List<Parameter> params = operation.getParameters();
        if (params == null || params.isEmpty()) {
            return 0;
        }

        // Get Request QueryParams
        String query = (String) message.get(Message.QUERY_STRING);
        String path = (String) message.get(Message.REQUEST_URI);
        Map<String, List<String>> qParams = JAXRSUtils.getStructuredParams(query, "&", true, false);
        Map<String, List<String>> mParams = JAXRSUtils.getMatrixParams(path, true);
        // Get Request Headers
        Map<?, ?> qHeader = (java.util.Map<?, ?>) message.get(Message.PROTOCOL_HEADERS);

        int rate = 0;
        for (Parameter p : params) {
            switch (p.getType()) {
                case QUERY:
                    if (qParams.containsKey(p.getName())) {
                        rate += 2;
                    } else if (p.getDefaultValue() == null) {
                        rate -= 1;
                    }
                    break;
                case MATRIX:
                    if (mParams.containsKey(p.getName())) {
                        rate += 2;
                    } else if (p.getDefaultValue() == null) {
                        rate -= 1;
                    }
                    break;
                case HEADER:
                    if (qHeader.containsKey(p.getName())) {
                        rate += 2;
                    } else if (p.getDefaultValue() == null) {
                        rate -= 1;
                    }
                    break;
                default:
                    break;
            }
        }
        return rate;
    }
}
