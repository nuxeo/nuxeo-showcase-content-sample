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

import static org.nuxeo.audit.api.LogEntryConstants.LOG_COMMENT;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class ShowcaseContentImporter {

    public static final String INITIALIZED_EVENT = "ShowcaseContentImported";

    /**
     * When computing event name for the "default" contribution; it uses the old global event name in case the showcase
     * content was already imported.
     * 
     * @deprecated since 2025.0, unused since we fire an event with {@link #INITIALIZED_EVENT} name
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String DEFAULT_NAME = "default";

    /**
     * @since 8.4
     * @deprecated since 2025.0, we now fire an event with {@link EventService} with {@link #INITIALIZED_EVENT} name and
     *             leverage audit contribution, the importer name is given as the {@link LogEntry#getComment()}
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String INITIALIZED_EVENT_FORMAT = "ShowcaseContentImported_%s";

    private static final Log log = LogFactory.getLog(ShowcaseContentImporter.class);

    protected String name;

    protected CoreSession session;

    protected ShowcaseContentImporter(CoreSession session, String name) {
        this.session = session;
        this.name = name;
    }

    public static void run(CoreSession session, String name, Blob blob) throws IOException {
        new ShowcaseContentImporter(session, name).create(blob);
    }

    public DocumentModel create(Blob blob) throws IOException {
        if (isImported()) {
            log.debug(String.format("Showcase Content '%s' already imported.", name));
            return null;
        }

        DocumentModel doc = create(session, blob, getImportPathRoot(), true);

        markImportDone();
        return doc;
    }

    protected DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite)
            throws IOException {
        try (CloseableFile source = content.getCloseableFile(".zip")) {
            try (ZipFile zip = ExportedZipImporter.getArchiveFileIfValid(source.getFile())) {
                if (zip == null) {
                    return null;
                }
            }

            boolean importWithIds = false;
            DocumentReader reader = new NuxeoArchiveReader(source.getFile());
            ExportedDocument root = reader.read();
            IdRef rootRef = new IdRef(root.getId());

            if (documentManager.exists(rootRef)) {
                DocumentModel target = documentManager.getDocument(rootRef);
                if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                    importWithIds = true;
                }
            }
            reader.close();

            DocumentRef resultingRef;
            if (overwrite && importWithIds) {
                resultingRef = rootRef;
            } else {
                String rootName = root.getPath().lastSegment();
                resultingRef = new PathRef(path, rootName);
            }

            DocumentWriter writer = new ShowcaseWriter(documentManager, path, 10);
            reader = new NuxeoArchiveReader(source.getFile());
            try {
                DocumentPipe pipe = new DocumentPipeImpl(10);
                pipe.setReader(reader);
                pipe.setWriter(writer);
                pipe.run();
            } catch (IOException e) {
                log.warn(e, e);
                return null;
            } finally {
                reader.close();
                writer.close();
            }
            return documentManager.getDocument(resultingRef);
        }
    }

    protected boolean isImported() {
        return !Framework.getService(AuditBackend.class)
                         .queryLogs(new AuditQueryBuilder()
                                                           .predicate(Predicates.and(
                                                                   Predicates.eq(LOG_EVENT_ID, INITIALIZED_EVENT),
                                                                   Predicates.eq(LOG_COMMENT, getLogEntryComment())))
                                                           .limit(1L))
                         .isEmpty();
    }

    protected void markImportDone() {
        var eventContext = new EventContextImpl();
        eventContext.setProperty("comment", getLogEntryComment());
        Framework.getService(EventService.class).fireEvent(eventContext.newEvent(INITIALIZED_EVENT));
    }

    protected String getLogEntryComment() {
        return "Showcase name: " + name;
    }

    protected String getImportPathRoot() {
        return session.query("Select * from Domain").get(0).getPathAsString();
    }
}
