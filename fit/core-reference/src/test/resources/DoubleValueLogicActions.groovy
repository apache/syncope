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
import groovy.transform.CompileStatic
import org.apache.syncope.common.lib.Attr
import org.apache.syncope.common.lib.request.AnyCR
import org.apache.syncope.common.lib.request.AnyUR
import org.apache.syncope.common.lib.request.AttrPatch
import org.apache.syncope.core.logic.api.LogicActions

/**
 * Class for integration tests: transform (by making it double) any attribute value for defined schema.
 */
@CompileStatic
class DoubleValueLogicActions implements LogicActions {
  
  private static final String NAME = "makeItDouble";

  @Override
  <C extends AnyCR> C beforeCreate(final C input) {
    for (Attr attr : input.getPlainAttrs()) {
      if (NAME.equals(attr.getSchema())) {
        List<String> values = new ArrayList<String>(attr.getValues().size());
        for (String value : attr.getValues()) {
          try {
            values.add(String.valueOf(2 * Long.parseLong(value)));
          } catch (NumberFormatException e) {
            // ignore
          }
        }
        attr.getValues().clear();
        attr.getValues().addAll(values);
      }
    }

    return input;
  }

  @Override
  <R extends AnyUR> R beforeUpdate(final R input) {
    for (AttrPatch patch : input.getPlainAttrs()) {
      if (NAME.equals(patch.getAttr().getSchema())) {
        List<String> values = new ArrayList<String>(patch.getAttr().getValues().size());
        for (String value : patch.getAttr().getValues()) {
          try {
            values.add(String.valueOf(2 * Long.parseLong(value)));
          } catch (NumberFormatException e) {
            // ignore
          }
        }
        patch.getAttr().getValues().clear();
        patch.getAttr().getValues().addAll(values);
      }
    }

    return input;
  }
}