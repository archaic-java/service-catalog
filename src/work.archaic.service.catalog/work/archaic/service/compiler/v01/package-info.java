/**
 * Synchronous Java syntax analysis, version 01. Providers consume in-memory source and return
 * immutable diagnostics without resolving project dependencies or producing class files.
 * Coordinates count UTF-16 code units in the original text, before Java Unicode-escape translation.
 * This contract has no editor lifecycle, scheduling, transport, or provider-discovery requirement.
 * Published v01 signatures and semantics are immutable; incompatible changes require a new version.
 */
package work.archaic.service.compiler.v01;
