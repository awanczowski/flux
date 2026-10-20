/*
 * Copyright (c) 2024-2026 Progress Software Corporation and/or its subsidiaries or affiliates. All Rights Reserved.
 */
package com.marklogic.flux.impl.importdata;

import com.marklogic.flux.api.CompressionType;
import com.marklogic.flux.api.ExtractedTextFormat;
import com.marklogic.flux.api.GenericFilesImporter;
import com.marklogic.flux.impl.OptionsUtil;
import com.marklogic.spark.Options;
import picocli.CommandLine;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@CommandLine.Command(
    name = "import-files",
    description = "Read local, HDFS, and S3 files and write the contents of each file as a document in MarkLogic."
)
public class ImportFilesCommand extends AbstractImportFilesCommand<GenericFilesImporter> implements GenericFilesImporter {

    @CommandLine.Mixin
    private ReadGenericFilesParams readParams = new ReadGenericFilesParams();

    @CommandLine.Mixin
    private WriteGenericDocumentsParams writeParams = new WriteGenericDocumentsParams();

    @CommandLine.Option(
        names = "--streaming",
        description = "Causes files to be read from their source directly to MarkLogic. Intended for importing large " +
            "files that cannot be fully read into memory. Features that depend on " +
            "the data in the file, such as --uri-template, will not have any effect when this option is set."
    )
    private boolean streaming;

    @Override
    public GenericFilesImporter streaming() {
        this.streaming = true;
        return this;
    }

    @Override
    protected String getReadFormat() {
        return MARKLOGIC_CONNECTOR;
    }

    @Override
    protected ReadGenericFilesParams getReadParams() {
        readParams.setStreaming(this.streaming);
        return readParams;
    }

    @Override
    protected WriteGenericDocumentsParams getWriteParams() {
        writeParams.setStreaming(this.streaming);
        return writeParams;
    }


    @Override
    public GenericFilesImporter from(Consumer<ReadGenericFilesOptions> consumer) {
        consumer.accept(readParams);
        return this;
    }

    @Override
    public GenericFilesImporter from(String... paths) {
        readParams.paths(paths);
        return this;
    }

    @Override
    public GenericFilesImporter to(Consumer<WriteGenericDocumentsOptions> consumer) {
        consumer.accept(writeParams);
        return this;
    }

    public static class ReadGenericFilesParams extends ReadCompressibleFilesParams<ReadGenericFilesOptions> implements ReadGenericFilesOptions {

        @CommandLine.Option(names = "--compression", description = "When importing compressed files, specify the type of compression used. "
            + OptionsUtil.VALID_VALUES_DESCRIPTION)
        private CompressionType compressionType;

        @CommandLine.Option(names = "--encoding", description = "Specify an encoding when reading files.")
        private String encoding;

        @Override
        public ReadGenericFilesOptions compressionType(CompressionType compressionType) {
            this.compressionType = compressionType;
            return this;
        }

        @Override
        public ReadGenericFilesOptions encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        @Override
        public Map<String, String> makeOptions() {
            return OptionsUtil.addOptions(super.makeOptions(),
                Options.READ_FILES_COMPRESSION, compressionType != null ? compressionType.name() : null,
                Options.READ_FILES_ENCODING, encoding
            );
        }
    }

    public static class WriteGenericDocumentsParams extends WriteDocumentParams<WriteGenericDocumentsOptions> implements WriteGenericDocumentsOptions {

        @CommandLine.Option(
            names = "--document-type",
            description = "Forces a type for any document with an unrecognized URI extension. " + OptionsUtil.VALID_VALUES_DESCRIPTION
        )
        private com.marklogic.flux.api.DocumentType documentType;

        @CommandLine.Option(
            names = "--extract-text",
            description = "Specifies that text should be extracted from each file and saved as a separate document."
        )
        private boolean extractText;

        @CommandLine.Option(
            names = "--extracted-text-format",
            description = "Format of the extracted content: text (default) or html (Tika XHTML). " +
                "Does not change the JSON/XML document type. " + OptionsUtil.VALID_VALUES_DESCRIPTION
        )
        private ExtractedTextFormat extractedTextFormat;

        @CommandLine.Option(
            names = "--extracted-text-document-type",
            description = "Specifies the type of document to create with the extracted text. Defaults to 'json'; can be 'json' or 'xml'."
        )
        private String extractedTextDocumentType;

        @CommandLine.Option(
            names = "--extracted-text-collections",
            description = "Comma-delimited sequence of collection names to add to each extracted text document."
        )
        private String extractedTextCollections;

        @CommandLine.Option(
            names = "--extracted-text-permissions",
            description = "Comma-delimited sequence of MarkLogic role names and capabilities to add to each extracted " +
                "text document - e.g. role1,read,role2,update,role3,execute."
        )
        private String extractedTextPermissions;

        @CommandLine.Option(
            names = "--extracted-text-drop-source",
            description = "Specify this to not write the content that text was extracted from as its own document."
        )
        private boolean extractedTextDropSource;

        @CommandLine.Option(
            names = "--extracted-text-aggregate-element",
            description = "Split the extracted XHTML into records, writing one document per occurrence of this element " +
                "instead of a single extracted text document. Requires '--extracted-text-format html' and " +
                "'--extracted-text-document-type xml'; cannot be combined with splitter options."
        )
        private String extractedTextAggregateElement;

        @CommandLine.Option(
            names = "--extracted-text-aggregate-namespace",
            description = "Namespace of the element specified by '--extracted-text-aggregate-element'; defaults to the XHTML namespace."
        )
        private String extractedTextAggregateNamespace;

        @CommandLine.Option(
            names = "--extracted-text-aggregate-attribute",
            description = "Only split out elements having this attribute value, specified as name=value - e.g. class=sheet."
        )
        private String extractedTextAggregateAttribute;

        @CommandLine.Option(
            names = "--extracted-text-aggregate-uri-attribute",
            description = "Name of an attribute on each record element whose value is used in the record's URI instead " +
                "of the record's position. The value must be present and unique within each source file."
        )
        private String extractedTextAggregateUriAttribute;

        @CommandLine.Option(
            names = "--extracted-text-aggregate-envelope",
            description = "Wrap each record in an envelope containing the source URI, record index, extracted metadata, " +
                "and - when a classifier is configured - the record's own classification. Required when a classifier is configured."
        )
        private boolean extractedTextAggregateEnvelope;

        @Override
        public WriteGenericDocumentsOptions documentType(com.marklogic.flux.api.DocumentType documentType) {
            this.documentType = documentType;
            return this;
        }

        @Override
        public Map<String, String> makeOptions() {
            return OptionsUtil.addOptions(super.makeOptions(),
                Options.WRITE_DOCUMENT_TYPE, documentType != null ? documentType.name() : null,
                Options.WRITE_EXTRACTED_TEXT, extractText ? "true" : null,
                Options.WRITE_EXTRACTED_TEXT_FORMAT, extractedTextFormat != null ? extractedTextFormat.name() : null,
                Options.WRITE_EXTRACTED_TEXT_DOCUMENT_TYPE, extractedTextDocumentType,
                Options.WRITE_EXTRACTED_TEXT_COLLECTIONS, extractedTextCollections,
                Options.WRITE_EXTRACTED_TEXT_PERMISSIONS, extractedTextPermissions,
                Options.WRITE_EXTRACTED_TEXT_DROP_SOURCE, extractedTextDropSource ? "true" : null,
                Options.WRITE_EXTRACTED_TEXT_AGGREGATE_ELEMENT, extractedTextAggregateElement,
                Options.WRITE_EXTRACTED_TEXT_AGGREGATE_NAMESPACE, extractedTextAggregateNamespace,
                Options.WRITE_EXTRACTED_TEXT_AGGREGATE_ATTRIBUTE, extractedTextAggregateAttribute,
                Options.WRITE_EXTRACTED_TEXT_AGGREGATE_URI_ATTRIBUTE, extractedTextAggregateUriAttribute,
                Options.WRITE_EXTRACTED_TEXT_AGGREGATE_ENVELOPE, extractedTextAggregateEnvelope ? "true" : null
            );
        }

        @Override
        public WriteGenericDocumentsOptions extractText() {
            this.extractText = true;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextFormat(ExtractedTextFormat format) {
            this.extractedTextFormat = Objects.requireNonNull(format, "Extracted text format must not be null");
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextDocumentType(String documentType) {
            this.extractedTextDocumentType = documentType;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextCollections(String commaDelimitedCollections) {
            this.extractedTextCollections = commaDelimitedCollections;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextPermissionsString(String rolesAndCapabilities) {
            this.extractedTextPermissions = rolesAndCapabilities;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextDropSource() {
            this.extractedTextDropSource = true;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextAggregateElement(String namespace, String element) {
            this.extractedTextAggregateNamespace = namespace;
            this.extractedTextAggregateElement = element;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextAggregateAttribute(String name, String value) {
            this.extractedTextAggregateAttribute = name + "=" + value;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextAggregateUriAttribute(String name) {
            this.extractedTextAggregateUriAttribute = name;
            return this;
        }

        @Override
        public WriteGenericDocumentsOptions extractedTextAggregateEnvelope() {
            this.extractedTextAggregateEnvelope = true;
            return this;
        }
    }
}
