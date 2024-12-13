/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.showcase.content.service;

import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import java.net.URL;

import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Descriptor;

@XObject(value = "content")
public class ShowcaseContentDescriptor implements Descriptor {

    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributingComponent;

    @XNode("@name")
    protected String name;

    @XNode("filename")
    protected String filename;

    @XNode("@enable")
    protected boolean enabled = true;

    protected URL blobUrl;

    @Override
    public String getId() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public URL getBlobUrl() {
        if (blobUrl == null) {
            blobUrl = contributingComponent.getRuntimeContext().getResource(filename);
        }
        return blobUrl;
    }

}
