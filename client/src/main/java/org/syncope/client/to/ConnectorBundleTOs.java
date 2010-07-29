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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class ConnectorBundleTOs extends AbstractBaseBean
        implements Iterable<ConnectorBundleTO> {

    private List<ConnectorBundleTO> bundles;

    public List<ConnectorBundleTO> getBundles() {
        if (this.bundles == null)
            this.bundles = new ArrayList<ConnectorBundleTO>();
        return this.bundles;
    }

    public boolean addBundle(ConnectorBundleTO bundle) {
        if (this.bundles == null)
            this.bundles = new ArrayList<ConnectorBundleTO>();

        return this.bundles.add(bundle);
    }

    public boolean removeBundle(ConnectorBundleTO bundle) {
        if (this.bundles == null) return true;
        return this.bundles.remove(bundle);
    }

    public void setBundles(List<ConnectorBundleTO> bundles) {
        this.bundles = bundles;
    }

    @Override
    public Iterator<ConnectorBundleTO> iterator() {
        if (this.bundles == null) {
            this.bundles = new ArrayList<ConnectorBundleTO>();
        }

        return this.bundles.iterator();
    }
}
