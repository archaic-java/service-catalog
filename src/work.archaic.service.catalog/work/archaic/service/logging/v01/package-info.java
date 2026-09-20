/**
 * Goal-scoped diagnostics, version 01. Reusable goals create independent execution state;
 * trails collect bounded evidence and logs publish immediate information and completed failure
 * reports. Execution owns
 * completion. There are no severity levels, MDC, goal inheritance, HTTP policy, JFR events or
 * tracing protocol types in this version. Runtime scope binding, virtual-thread scheduling and
 * output implementations belong in provider libraries. Once published, incompatible signature
 * or semantic changes require a new versioned package.
 */
package work.archaic.service.logging.v01;
