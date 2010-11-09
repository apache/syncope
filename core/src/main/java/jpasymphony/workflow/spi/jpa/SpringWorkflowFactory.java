/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package jpasymphony.workflow.spi.jpa;

import com.opensymphony.workflow.FactoryException;
import com.opensymphony.workflow.loader.XMLWorkflowFactory;
import java.util.Properties;

public class SpringWorkflowFactory extends XMLWorkflowFactory {

    private String resource;

    public void setReload(String reload) {
        this.reload = Boolean.valueOf(reload).booleanValue();
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void init() {
        try {
            Properties props = new Properties();
            props.setProperty("reload", getReload());
            props.setProperty("resource", getResource());

            super.init(props);
            initDone();
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    private String getReload() {
        return String.valueOf(reload);
    }

    private String getResource() {
        return resource;
    }
}
