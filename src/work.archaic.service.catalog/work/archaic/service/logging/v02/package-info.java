/**
 * Goal-scoped diagnostics, version 02. Goals describe application intents; each attempt has independent execution state.
 * Trails collect bounded evidence and logs publish immediate information and completed failure
 * reports. Execution owns completion. There are no severity levels, MDC, goal inheritance, HTTP policy, JFR events or
 * tracing protocol types in this version. Runtime scope binding, virtual-thread scheduling and
 * output implementations belong in provider libraries. Once published, incompatible signature
 * or semantic changes require a new versioned package.
 */
package work.archaic.service.logging.v02;

