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
import groovy.transform.BaseScript
import org.apache.syncope.core.provisioning.api.macro.MacroActions
import java.io.Serializable

abstract class EvalOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript EvalOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  output.append(groovy.util.Eval.me("'eval-me0|'"))
  output.append(groovy.util.Eval.me('value', 'eval-me1|', 'value'))
  output.append(groovy.util.Eval.x('eval-x|', 'x'))
  output.append(groovy.util.Eval.xy('eval-', 'xy|', 'x + y'))
  output.append(groovy.util.Eval.xyz('eval-', 'x', 'yz|', 'x + y + z'))
  return output
}
