# Compiler contract v01

`work.archaic.service.compiler.v01` defines synchronous syntax analysis of in-memory Java source.
It has no JSON, LSP, logging, test-framework or scheduler dependency.

Consumers supply `SourceSnapshot(uri, fileName, text)` and invoke `CompilerAdapter.parse`.
Providers return `ParseResult` with the original source identity, immutable diagnostics and
source-less notices. Ordinary Java syntax errors are successful results containing diagnostics;
infrastructure failures throw `ParseException`. Providers document their supported language level.

`Position` counts zero-based lines and UTF-16 code units in the original text. A tab occupies one
code unit. Ranges are half-open and can be zero-width at EOF. Providers must account for LF, CRLF,
CR and Java Unicode escapes without returning coordinates in the translated Java source.

The capability must not read source files, resolve project dependencies, run annotation processors,
or create class files. Calls are independent and safe to invoke concurrently. Providers own and
release their compiler resources per invocation. Empty diagnostics do not attest type correctness.

Document versions and close/reopen generations belong to the consumer. A consumer can retain
the request alongside an asynchronous task and validate its lifecycle before publishing the result.
These application-specific tokens are deliberately not part of the compiler contract.

The first provider is `work.archaic.shrink.compiler.JavacCompiler` in
[shrink](https://github.com/archaic-java/shrink). It is selected explicitly by construction;
this contract does not require a ServiceLoader registry. Its contract-level tests live in shrink
and exercise the provider through `CompilerAdapter`, including independent JPMS consumption.

Once published, v01 remains immutable. Breaking type or behavioral changes require a new
versioned package. Existing catalog contracts are unchanged.
