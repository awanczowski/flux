---
layout: default
title: Importing generic files
parent: Importing files
grand_parent: Importing Data
nav_order: 2
---

Flux can import any type of file as-is, with the contents of the file becoming a new document in MarkLogic. The term
"generic files" is used in this context to refer to files that do not require any special processing
other than potentially decompressing the files.

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}

## Usage

The `import-files` command imports a set of files into MarkLogic, with each file being written as a separate
document. You must specify at least one `--path` option along with connection information for the MarkLogic database
you wish to write to. For example:

{% tabs log %}
{% tab log Unix %}
```
./bin/flux import-files \
    --path /path/to/files \
    --connection-string "flux-example-user:password@localhost:8004" \
    --permissions flux-example-role,read,flux-example-role,update
```
{% endtab %}
{% tab log Windows %}
```
bin\flux import-files ^
    --path path\to\files ^
    --connection-string "flux-example-user:password@localhost:8004" ^
    --permissions flux-example-role,read,flux-example-role,update
```
{% endtab %}
{% endtabs %}


## Controlling document URIs

Each document will have an initial URI based on the absolute path of the associated file. See
[common import features](../common-import-features.md) for details on adjusting this URI. In particular, the
`--uri-replace` option is often useful for removing most of the absolute path to produce a concise, self-describing
URI.

## Specifying a document type

The type of each document written to MarkLogic is determined by the file extension found in the URI along with the
set of [MIME types configured in MarkLogic](https://docs.marklogic.com/admin-help/mimetype). For unrecognized file
extensions, or URIs that do not have a file extension, you can force a document type via the `--document-type` option.
The value of this option must be one of `JSON`, `XML`, or `TEXT`.

## Specifying an encoding

MarkLogic stores all content [in the UTF-8 encoding](https://docs.marklogic.com/guide/search-dev/encodings_collations#id_87576).
If your files use a different encoding, you must specify that via the `--encoding` option so that
the content can be correctly translated to UTF-8 when written to MarkLogic:

{% tabs log %}
{% tab log Unix %}
```
./bin/flux import-files \
    --path source \
    --encoding ISO-8859-1 \
    --connection-string "flux-example-user:password@localhost:8004" \
    --permissions flux-example-role,read,flux-example-role,update
```
{% endtab %}
{% tab log Windows %}
```
bin\flux import-files ^
    --path source ^
    --encoding ISO-8859-1 ^
    --connection-string "flux-example-user:password@localhost:8004" ^
    --permissions flux-example-role,read,flux-example-role,update
```
{% endtab %}
{% endtabs %}


## Importing large binary files

Flux can leverage MarkLogic's [support for large binary documents](https://docs.marklogic.com/guide/app-dev/binaries#id_93203)
by importing binary files of any size. To ensure that binary files of any size can be loaded, consider using the
`--streaming` option introduced in Flux 1.1.0. When this option is set, Flux will stream the contents of each file from
its source directly into MarkLogic, thereby avoiding reading the contents of a file into memory.

As streaming a file requires Flux to only send one document at a time to MarkLogic, you should not use this option when
importing smaller files that easily fit into the memory available to Flux.

When using `--streaming`, the following options will have no effect due to Flux not reading the file contents into
memory and always sending one file per request to MarkLogic:

- `--batch-size`
- `--encoding`
- `--failed-documents-path`
- `--uri-template`

You typically will also not want to use the `--transform` option as applying a REST transform in MarkLogic to a very
large binary document may exhaust the amount of memory available to MarkLogic.

In addition, when streaming documents to MarkLogic, URIs will be encoded. For example, a file named `my file.json`
will result in a URI of `/my%20file.json`. This is due to an
[issue in the MarkLogic REST API endpoint](https://docs.marklogic.com/REST/PUT/v1/documents) that will be resolved in
a future server release.

## Importing gzip files

To import gzip files with each file being decompressed before written to MarkLogic, include the `--compression` option
with a value of `GZIP`. You can also import gzip files as-is - i.e. without decompressing them - by not including the
`--compression` option. The `--streaming` option introduced in Flux 1.1.0 can also be used for very large gzip files
that may not fit into the memory available to Flux or to MarkLogic.

## Importing ZIP files

To import each entry in a ZIP file as a separate document, include the `--compression` option with a value of `ZIP`.
Each document will have an initial URI based on both the absolute path of the ZIP file and the name of the ZIP entry.
You can also use the `--document-type` option as described above to force a document type for any entry that has a file
extension not recognized by MarkLogic. The `--streaming` option introduced in Flux 1.1.0 can also be used for ZIP files
containing very large binary files that may not fit into the memory available to Flux or to MarkLogic.

## Extracting text

As of Flux 1.3.0, text can be extracted from files via [Apache Tika](https://tika.apache.org/) and written as separate
documents in MarkLogic. This is typically useful when importing binary content such as PDF and Word files, where both
the binary file and extracted text can be stored in MarkLogic.

### Behavior change in Flux 2.1.2: Microsoft Office file extraction

Flux 2.1.2 upgrades Apache Tika from 3.3.1 to 3.3.2. Tika 3.3.2 changes the default parser for Microsoft Office OOXML
files (`.docx`, `.pptx`, `.xlsx`, `.vsdx`) from a DOM-based extractor to a SAX-based extractor. The SAX parser is
faster and more memory-efficient, but may produce slightly different whitespace in extracted text compared to previous
Flux versions — for example, paragraph separators may differ.

If your application depends on the exact text output from Office files and you need the previous DOM-based behavior,
you can restore it by providing a [Tika configuration file](https://tika.apache.org/3.3.2/configuring.html) that
sets `useSAXDocxExtractor` and/or `useSAXPptxExtractor` to `false` on the `OfficeParserConfig`.

Text extraction is enabled by including the following option when executing the `import-files` command:

    --extract-text

When included, Flux will extract text from each file and create a separate document based on the following default
behavior:

1. The document will be JSON with a URI equalling that of the binary document URI plus `-extracted-text.json`.
2. The document will inherit any permissions assigned to the binary document.
3. The document will not inherit any collections assigned to the binary document.
4. The document will have keys of `source-uri`, `content`, and `extracted-metadata`.

The `content` in the document contains the extracted text. The `extracted-metadata` contains each metadata key and value
produced by Apache Tika.

### Choosing plain text or HTML content

Extracted content is plain text by default. To preserve the structure that Apache Tika extracts as XHTML, use:

    --extract-text --extracted-text-format html

Use `--extracted-text-format text` to explicitly select plain text. Values are case-insensitive; other values are
rejected. The format option only applies when `--extract-text` is enabled.

With `--extracted-text-document-type XML`, the XHTML is inserted into the document's `content` element as XML
elements (a root `html` element in the `http://www.w3.org/1999/xhtml` namespace), so it can be queried and indexed as
structure. If the XHTML cannot be parsed, it is stored as text and a warning is logged. With JSON documents, `content`
is a string containing the serialized XHTML. The URI and metadata are unchanged. Downstream splitting, classification,
and embedding of extracted content will receive the selected representation, including markup when HTML is selected.

The Java API provides the same selection via `.extractText().extractedTextFormat(ExtractedTextFormat.HTML)`.
This option requires the updated MarkLogic Spark connector (`3.1.3-SNAPSHOT` for local development); publish the
companion connector checkout to Maven Local before building Flux.

### Splitting extracted HTML into records

Instead of writing one extracted text document per file, the extracted XHTML can be split into individual records using
the same streaming splitter as the `import-aggregate-xml-files` command. For example, Tika produces one
`<div class="sheet">` element per Excel worksheet, so the following writes one document per worksheet:

    --extract-text
    --extracted-text-format html
    --extracted-text-document-type XML
    --extracted-text-aggregate-element div
    --extracted-text-aggregate-attribute class=sheet

The following options control how records are produced:

| Option | Description |
| --- | --- |
| `--extracted-text-aggregate-element` | Local name of the record element. Only the outermost match is a record; nested matches stay inside it. |
| `--extracted-text-aggregate-namespace` | Namespace of the record element; defaults to the XHTML namespace. |
| `--extracted-text-aggregate-attribute` | Only elements having this exact attribute value, given as `name=value`, are records. Without it, Tika's `<div class="package-entry">` elements for embedded files would also become records. |
| `--extracted-text-aggregate-uri-attribute` | Use the value of this attribute in each record URI instead of the record's position. The value must be present and unique within each file, otherwise an error is thrown. |
| `--extracted-text-aggregate-envelope` | Wrap each record in an envelope containing `source-uri`, `record-index`, the record under `content`, and `extracted-metadata`. |
| `--extracted-text-aggregate-collections` | Comma-delimited collections for each record; defaults to the value of `--collections`. |
| `--extracted-text-aggregate-permissions` | Comma-delimited roles and capabilities for each record; defaults to the value of `--permissions`. |
| `--extracted-text-aggregate-keep-document` | Also write the full extracted text document, and link each record to it. |

By default, records replace the single extracted text document. Each record has a URI of
`{sourceUri}-extracted-{position}.xml`, with position starting at 1, or `{sourceUri}-extracted-{attributeValue}.xml` when
`--extracted-text-aggregate-uri-attribute` is used. Unlike the full extracted text document, records default to the
`--collections` and `--permissions` values of the source file. Without the envelope, each record document is the record
element itself, such as `<div xmlns="http://www.w3.org/1999/xhtml" class="sheet">`.

Every record links back to the file it was extracted from - even when `--extracted-text-drop-source` is used - via a
`source-uri` metadata value, and a `source-uri` element in the envelope. With `--extracted-text-aggregate-keep-document`,
the full extracted text document is also written, and each record has an `extracted-text-uri` metadata value and
envelope element containing that document's URI. Like the records, the kept full document defaults to the
`--collections` and `--permissions` values, unless `--extracted-text-collections` or `--extracted-text-permissions` are
specified.

The record options require `--extracted-text-format html` and `--extracted-text-document-type XML`, and cannot be
combined with the `--splitter-*` options. If no element matches in a file, a warning is logged and the single extracted
text document is written for that file instead.

When a classifier is configured with the `--classifier-*` options, each record is classified individually by Semaphore.
The text of each record is sent as one article in a multi-article request, batched according to
`--classifier-batch-size`, and the response is added as a `classification` element in that record's envelope. The file
as a whole is only sent to Semaphore when `--extracted-text-aggregate-keep-document` is used, so that the full extracted
text document is classified as well. Because a record without an envelope has no place to
hold its classification, `--extracted-text-aggregate-envelope` is required when a classifier is configured.

The Java API provides the same options via `extractedTextAggregateElement`, `extractedTextAggregateAttribute`,
`extractedTextAggregateUriAttribute`, `extractedTextAggregateEnvelope`, `extractedTextAggregateCollections`,
`extractedTextAggregatePermissionsString`, and `extractedTextAggregateKeepDocument`.

If you do not want Flux to write the file to a separate document but only want the extracted text document written,
include the following option:

    --extracted-text-drop-source

### Writing extracted text XML documents

Extracted text documents can be written as XML instead of JSON by including the following option:

    --extracted-text-document-type XML

When writing Tika metadata to the extracted text document, Flux will attempt to determine a well-known namespace based
on the name of the metadata key. For example, a Tika metadata key that begins with `pdf:` will result in an element
assigned to the `http://ns.adobe.com/pdf/1.3/` namespace.

### Assigning metadata to extracted text documents

Collections can be assigned to extracted text documents via the following option, which accepts a comma-delimited list
of collection names:

    --extracted-text-collections collection1,collection2

Permissions can be assigned to extracted text documents via the following options, which accepts a comma-delimited
sequence of MarkLogic role names and capabilities:

    --extracted-text-permissions role1,read,role2,update

## Zip bomb protection

When importing ZIP files from untrusted sources, you can protect against
[zip bombs](https://en.wikipedia.org/wiki/Zip_bomb) — archives crafted to expand to an enormous amount of data
and potentially exhaust the memory available to Flux. Both options below are disabled by default.

To limit the maximum number of uncompressed bytes read from any single ZIP entry:

    --zip-max-uncompressed-entry-bytes 268435456

A value of `268435456` (256 MB) is a reasonable starting point for most use cases. If a single entry exceeds
this limit, an error is thrown. Set to `0` or any value less than `1` to disable.

To limit the maximum number of entries processed from a single ZIP file:

    --zip-max-entry-count 100000

A value of `100000` is a reasonable starting point for most use cases. Set to `0` or any value less than `1` to
disable.
