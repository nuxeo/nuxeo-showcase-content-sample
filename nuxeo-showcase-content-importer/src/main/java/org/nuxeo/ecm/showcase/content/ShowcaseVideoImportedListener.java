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

package org.nuxeo.ecm.showcase.content;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_IMPORTED;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.service.VideoInfoWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Override default org.nuxeo.ecm.platform.video.listener.VideoChangedListener listener as an async/postcommit one to
 * ensure the Blob is present in the DocumentModel, because is not on the synchronised documentImported event.
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class ShowcaseVideoImportedListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(ShowcaseVideoImportedListener.class);

    @Override
    public void handleEvent(EventBundle eventBundle) {
        if (eventBundle.containsEventName(DOCUMENT_IMPORTED)) {
            eventBundle.forEach(this::handle);
        }
    }

    protected void handle(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (doc.hasFacet(HAS_VIDEO_PREVIEW_FACET) && !doc.isProxy()) {
            try {
                resetProperties(doc);
                doc.putContextData("disableVideoConversionsGenerationListener", true);
                doc = ctx.getCoreSession().saveDocument(doc);
            } catch (IOException e) {
                throw new NuxeoException(
                        String.format("Error while resetting video properties of document %s.", doc), e);
            }
            scheduleAsyncProcessing(doc);
        }
    }

    protected void resetProperties(DocumentModel doc) throws IOException {
        log.debug(String.format("Resetting video info, storyboard, previews and conversions of document %s", doc));
        VideoHelper.updateVideoInfo(doc, null);
        VideoHelper.updateStoryboard(doc, null);
        VideoHelper.updatePreviews(doc, null);
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, null);
    }

    protected void scheduleAsyncProcessing(DocumentModel doc) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        VideoInfoWork work = new VideoInfoWork(doc.getRepositoryName(), doc.getId());
        log.debug(String.format("Scheduling work: video info of document %s.", doc));
        workManager.schedule(work, true);
    }
}
