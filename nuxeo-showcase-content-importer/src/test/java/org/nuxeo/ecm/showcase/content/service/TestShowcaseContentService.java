/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.showcase.content.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.content.showcase")
@Deploy("org.nuxeo.ecm.platform.thumbnail")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.content.showcase:contrib.xml")
public class TestShowcaseContentService {

    public static final String DOC_ID = "921f3887-6270-49ea-bec0-2dd48ba44a89";

    @Inject
    protected ShowcaseContentService showcaseContentService;

    @Inject
    protected CoreSession session;

    @Test
    public void testService() {
        assertNotNull(showcaseContentService);
    }

    @Test
    public void testContribution() {
        assertEquals(0, session.query("select * from File").size());

        List<ShowcaseContentDescriptor> c = ((ShowcaseContentServiceImpl) showcaseContentService).getContributions();
        assertEquals(1, c.size());

        showcaseContentService.triggerImporters(session);

        DocumentModelList docs = session.query("select * from File");
        assertEquals(1, docs.size());
        assertTrue(docs.stream().anyMatch(s -> s.getId().equals(DOC_ID)));

        docs = session.query("select * from Note");
        assertEquals(1, docs.size());
        assertTrue(docs.get(0).getPropertyValue("dublincore:creator").equals("arthur"));
    }
}
