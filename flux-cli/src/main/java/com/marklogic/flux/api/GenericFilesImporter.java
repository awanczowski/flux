/*
 * Copyright (c) 2024-2026 Progress Software Corporation and/or its subsidiaries or affiliates. All Rights Reserved.
 */
package com.marklogic.flux.api;

import java.util.function.Consumer;

/**
 * Read local, HDFS, and S3 files and write the contents of each file as a document in MarkLogic.
 */
public interface GenericFilesImporter extends Executor<GenericFilesImporter> {

    interface ReadGenericFilesOptions extends ReadCompressibleFilesOptions<ReadGenericFilesOptions> {
        ReadGenericFilesOptions compressionType(CompressionType compressionType);

        ReadGenericFilesOptions encoding(String encoding);
    }

    interface WriteGenericDocumentsOptions extends WriteDocumentsOptions<WriteGenericDocumentsOptions> {
        /**
         * @since 1.4.0
         */
        WriteGenericDocumentsOptions documentType(com.marklogic.flux.api.DocumentType documentType);

        /**
         * @since 1.3.0
         */
        WriteGenericDocumentsOptions extractText();

        /**
         * Select plain text (the default) or Tika XHTML for the extracted document's content.
         *
         * @param format the extracted content format; must not be null
         */
        WriteGenericDocumentsOptions extractedTextFormat(ExtractedTextFormat format);

        /**
         * @param documentType can be "json" or "xml"; defaults to "json" if not specified or if an unrecognized value is provided.
         * @since 1.4.0
         */
        WriteGenericDocumentsOptions extractedTextDocumentType(String documentType);

        /**
         * @since 1.3.0
         */
        WriteGenericDocumentsOptions extractedTextCollections(String commaDelimitedCollections);

        /**
         * @since 1.3.0
         */
        WriteGenericDocumentsOptions extractedTextPermissionsString(String rolesAndCapabilities);

        /**
         * @since 1.3.0
         */
        WriteGenericDocumentsOptions extractedTextDropSource();

        /**
         * Split the extracted XHTML into records, writing one XML document per occurrence of the given element instead
         * of a single extracted text document. Requires an extracted text format of HTML and an extracted text
         * document type of "xml". Records are given URIs based on their position unless
         * {@link #extractedTextAggregateUriAttribute(String)} is used.
         *
         * @param namespace optional namespace of the element; defaults to the XHTML namespace if null
         * @param element   local name of the element, e.g. "div"
         */
        WriteGenericDocumentsOptions extractedTextAggregateElement(String namespace, String element);

        /**
         * Only split out record elements having an attribute with the given name and exact value, e.g. class=sheet.
         */
        WriteGenericDocumentsOptions extractedTextAggregateAttribute(String name, String value);

        /**
         * @param name attribute on each record element whose value is used in the record URI instead of its position;
         *             the value must be present and unique within each source file
         */
        WriteGenericDocumentsOptions extractedTextAggregateUriAttribute(String name);

        /**
         * Wrap each record in an envelope containing the source URI, record index, and extracted metadata.
         */
        WriteGenericDocumentsOptions extractedTextAggregateEnvelope();

        /**
         * @param commaDelimitedCollections collections for each record; defaults to the collections for the source document
         */
        WriteGenericDocumentsOptions extractedTextAggregateCollections(String commaDelimitedCollections);

        /**
         * @param rolesAndCapabilities permissions for each record, e.g. role1,read,role2,update; defaults to the
         *                             permissions for the source document
         */
        WriteGenericDocumentsOptions extractedTextAggregatePermissionsString(String rolesAndCapabilities);

        /**
         * Also write the full extracted text document, with each record linking to it. By default, the records replace
         * the full extracted text document.
         */
        WriteGenericDocumentsOptions extractedTextAggregateKeepDocument();
    }

    GenericFilesImporter from(Consumer<ReadGenericFilesOptions> consumer);

    GenericFilesImporter from(String... paths);

    /**
     * @since 1.1.0
     */
    GenericFilesImporter streaming();

    GenericFilesImporter to(Consumer<WriteGenericDocumentsOptions> consumer);
}
