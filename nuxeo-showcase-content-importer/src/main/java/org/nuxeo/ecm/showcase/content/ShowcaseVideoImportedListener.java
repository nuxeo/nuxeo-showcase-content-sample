/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.showcase.content;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_IMPORTED;
import static org.nuxeo.ecm.platform.video.VideoConstants.HAS_VIDEO_PREVIEW_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_CHANGED_EVENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.video.VideoHelper;
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
            Property origVideoProperty = doc.getProperty("file:content");

            Blob video = (Blob) origVideoProperty.getValue();
            updateVideoInfo(doc, video);

            // only trigger the event if we really have a video
            if (video != null) {
                Event trigger = ctx.newEvent(VIDEO_CHANGED_EVENT);
                EventService eventService = Framework.getLocalService(EventService.class);
                eventService.fireEvent(trigger);
            }
        }
    }

    protected void updateVideoInfo(DocumentModel doc, Blob video) {
        try {
            VideoHelper.updateVideoInfo(doc, video);
        } catch (NuxeoException e) {
            // may happen if ffmpeg is not installed
            log.error(String.format("Unable to retrieve video info: %s", e.getMessage()));
            log.debug(e, e);
        }
    }
}
