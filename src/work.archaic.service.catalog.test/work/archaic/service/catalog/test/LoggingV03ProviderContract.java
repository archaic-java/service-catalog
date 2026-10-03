package work.archaic.service.catalog.test;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import work.archaic.service.logging.v03.*;
import work.archaic.service.logging.v03.Configuration;
import work.archaic.service.test.v02.*;

/** Reusable provider checks. Factories supply an independent provider, clock and thread-safe sinks. */
public final class LoggingV03ProviderContract {
    private LoggingV03ProviderContract() {}
    public record Fixture(Log log, Clock clock, List<Entry> entries, List<FailureReport> reports) {
        public Context context(boolean debug) {
            return log.context(new Configuration(debug, clock, entries::add, reports::add, 2, 32));
        }
    }
    public static void cases(Collection<TestCase> cases, Supplier<Fixture> factory) {
        cases.add(new SuccessfulContext(factory));
        cases.add(new EscapingContextFailure(factory, false));
        cases.add(new EscapingContextFailure(factory, true));
        cases.add(new MarkedContextFailure(factory, false));
        cases.add(new MarkedContextFailure(factory, true));
        cases.add(new NestedContexts(factory, false));
        cases.add(new NestedContexts(factory, true));
        cases.add(new LazyDebug(factory, false));
        cases.add(new LazyDebug(factory, true));
        cases.add(new DebugSupplierFailure(factory, false));
        cases.add(new DebugSupplierFailure(factory, true));
        cases.add(new ContextBounds(factory));
        cases.add(new InactiveContext(factory));
        cases.add(new ConcurrentContexts(factory));
        cases.add(new ThreadOwnership(factory));
        cases.add(new ContextInputValidation(factory));
        cases.add(new ObjectLogging(factory));
        for (boolean explicit : new boolean[]{false, true})
            for (boolean nullable : new boolean[]{false, true}) cases.add(new ReturningContext(factory, explicit, nullable));
        cases.add(new FailingCall(factory, false));
        cases.add(new FailingCall(factory, true));
        cases.add(new NestedCall(factory));
        cases.add(new CallLifecycle(factory));
    }
}

record SuccessfulContext(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        var context = f.context(false);
        Thread caller = Thread.currentThread();
        assert f.entries().isEmpty() && f.reports().isEmpty() : "Creating a context starts no work";
        context.run(() -> {
            assert Thread.currentThread() == caller : "Execute on the calling thread";
            assert Logging.context() == context : "Bind the running context";
            context.onFailure("worker", "temporary");
            context.immediately("worker", "visible");
        });
        assert f.entries().size() == 1 : "Publish immediate messages";
        assert f.reports().isEmpty() : "Successful context discards evidence";
        var next = f.context(false);
        next.run(() -> next.fail("next failure"));
        assert f.reports().getFirst().evidence().isEmpty() : "Successful evidence must not leak into later contexts";
        try { Logging.context(); assert false : "Restore unbound state after scope exit"; }
        catch (IllegalStateException expected) { }
    }
}

record EscapingContextFailure(Supplier<LoggingV03ProviderContract.Fixture> factory,
                             boolean error) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        var checked = new IOException("original");
        var fatal = new AssertionError("original error");
        try {
            context.run(() -> {
                context.onFailure("worker", "before");
                if (error) throw fatal;
                throw checked;
            });
            assert false : "Escaping failure must be rethrown";
        } catch (IOException caught) {
            assert !error && caught == checked : "Preserve checked exception identity";
        } catch (AssertionError caught) {
            assert error && caught == fatal : "Preserve error identity";
        }
        assert f.reports().size() == 1 : "Publish failed context exactly once";
        var report = f.reports().getFirst();
        assert report.cause() == (error ? fatal : checked) : "Retain original cause";
        assert report.evidence().getFirst().message().equals("before") : "Include preceding evidence";
        try { report.evidence().clear(); assert false : "Evidence must be immutable"; }
        catch (UnsupportedOperationException expected) { }
        try { Logging.context(); assert false : "Cleanup binding on exceptional completion"; }
        catch (IllegalStateException expected) { }
    }
}

record MarkedContextFailure(Supplier<LoggingV03ProviderContract.Fixture> factory,
                           boolean escape) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        var cause = new IOException("later exception");
        try {
            context.run(() -> {
                context.fail("first reason");
                context.fail("later reason");
                assert f.reports().isEmpty() : "Marking failure must defer publication until completion";
                context.onFailure("worker", "after failure");
                if (escape) throw cause;
            });
        } catch (IOException caught) {
            assert escape && caught == cause : "Explicit mark must not replace escaping exception";
        }
        assert f.reports().size() == 1 : "Multiple marks produce one report";
        var report = f.reports().getFirst();
        assert report.explicitFailure().message().equals("first reason") : "First explicit failure is sticky";
        assert report.cause() == (escape ? cause : null) : "Include both reason and cause when present";
        assert report.evidence().size() == 1 : "Collect evidence after a failure mark";
    }
}

record NestedContexts(Supplier<LoggingV03ProviderContract.Fixture> factory,
                      boolean recover) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        var outer = f.context(false); var inner = f.context(true);
        var cause = new IOException("inner failed");
        try {
            outer.run(() -> {
                outer.onFailure("outer", "outer evidence");
                try {
                    inner.run(() -> {
                        assert Logging.context() == inner : "Nested context becomes current";
                        try { outer.onFailure("outer", "suspended"); assert false : "Outer context is suspended while inner is current"; }
                        catch (IllegalStateException expected) { }
                        inner.onFailure("inner", "inner evidence");
                        inner.onDebug("inner", () -> "inner debug");
                        throw cause;
                    });
                } catch (IOException caught) {
                    assert Logging.context() == outer : "Restore parent binding before nested exception handling";
                    assert f.reports().size() == 1 : "Inner failure publishes its own independent report";
                    if (!recover) throw caught;
                }
                outer.onDebug("outer", () -> { throw new AssertionError("outer debug disabled"); });
            });
        } catch (IOException caught) {
            assert !recover && caught == cause : "Unrecovered child failure escapes the parent";
        }
        assert f.reports().size() == (recover ? 1 : 2) : "Only unrecovered inner failures fail the parent";
        assert f.reports().getFirst().evidence().getFirst().source().equals("inner") : "Inner report contains inner evidence only";
        if (!recover) {
            assert f.reports().getLast().evidence().size() == 1 : "Parent evidence remains independent";
            assert f.reports().getLast().evidence().getFirst().source().equals("outer") : "Preserve outer evidence";
        }
        assert f.entries().size() == 1 : "Each context has its own debug configuration";
    }
}

record LazyDebug(Supplier<LoggingV03ProviderContract.Fixture> factory,
                 boolean enabled) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(enabled);
        var calls = new AtomicInteger(); Thread caller = Thread.currentThread();
        context.run(() -> context.onDebug("worker", () -> {
            assert Thread.currentThread() == caller : "Debug computation stays on the calling thread";
            calls.incrementAndGet();
            return "expensive result";
        }));
        assert calls.get() == (enabled ? 1 : 0) : "Evaluate enabled debug exactly once; never compute disabled debug";
        assert f.entries().size() == (enabled ? 1 : 0) : "Publish enabled debug exactly once";
    }
}

record DebugSupplierFailure(Supplier<LoggingV03ProviderContract.Fixture> factory,
                            boolean returnsNull) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(true);
        var failure = new IllegalArgumentException("debug computation failed");
        try {
            context.run(() -> {
                context.onFailure("worker", "before debug");
                context.onDebug("worker", () -> {
                    if (returnsNull) return null;
                    throw failure;
                });
            });
            assert false : "Enabled debug supplier errors must stay observable";
        } catch (NullPointerException caught) {
            assert returnsNull : "Reject null result from enabled debug supplier";
        } catch (IllegalArgumentException caught) {
            assert !returnsNull && caught == failure : "Preserve supplier exception identity";
        }
        assert f.entries().isEmpty() : "Failed supplier must not publish an entry";
        assert f.reports().size() == 1 : "Escaping supplier failure fails the context";
        assert f.reports().getFirst().evidence().size() == 1 : "Retain evidence preceding supplier failure";
    }
}

record ContextBounds(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        String longMessage = "x".repeat(30) + "😀" + "x".repeat(10);
        context.run(() -> {
            context.onFailure("worker", "evicted");
            context.onFailure("worker", "retained");
            context.onFailure("s".repeat(100), longMessage);
            context.fail("failure");
        });
        var report = f.reports().getFirst();
        assert report.dropped() == 1 : "Report exact number of evicted entries";
        assert report.evidence().size() == 2 : "Bound retained evidence";
        assert report.evidence().getFirst().message().equals("retained") : "Keep most recent evidence";
        var last = report.evidence().getLast();
        assert last.source().length() <= 32 : "Bound retained source names";
        assert last.message().length() <= 32 && last.message().endsWith("…") : "Mark clipped fields";
        assert !Character.isHighSurrogate(last.message().charAt(last.message().length()-2)) : "Do not split surrogate pairs";
    }
}

record InactiveContext(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        for (boolean completed : List.of(false, true)) {
            if (completed) context.run(() -> {});
            try { context.onFailure("worker", "lost"); assert false : "Reject evidence outside active scope"; }
            catch (IllegalStateException expected) { }
            try { context.immediately("worker", "lost"); assert false : "Immediate output requires active context"; }
            catch (IllegalStateException expected) { }
            try { context.fail("lost"); assert false : "Reject failure marks outside active scope"; }
            catch (IllegalStateException expected) { }
            try { context.onDebug("worker", () -> "lost"); assert false : "Disabled debug also requires active scope"; }
            catch (IllegalStateException expected) { }
        }
        try { context.run(() -> {}); assert false : "Completed context must not reopen"; }
        catch (IllegalStateException expected) { }
        var failed = f.context(false);
        try { failed.run(() -> { throw new IOException("failure"); }); }
        catch (IOException expected) { }
        try { failed.run(() -> {}); assert false : "Failed context must not reopen"; }
        catch (IllegalStateException expected) { }
    }
}

record ConcurrentContexts(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var ready = new CountDownLatch(2);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (String name : List.of("one", "two")) {
                var context = f.context(false);
                futures.add(executor.submit(() -> {
                    context.run(() -> {
                        context.onFailure(name, name);
                        ready.countDown();
                        assert ready.await(5, TimeUnit.SECONDS) : "Independent contexts must overlap";
                        context.fail(name);
                    });
                    return null;
                }));
            }
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        }
        assert f.reports().size() == 2 : "Each execution produces its own report";
        for (var report : f.reports()) {
            assert report.evidence().size() == 1 : "Concurrent contexts must not share evidence";
            assert report.evidence().getFirst().message().equals(report.explicitFailure().message()) : "Associate evidence with its failure";
        }
    }
}

record ThreadOwnership(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var parent = f.context(false);
        parent.run(() -> {
            var task = new java.util.concurrent.FutureTask<Void>(() -> {
                try { Logging.context(); assert false : "Child threads must not inherit the parent context"; }
                catch (IllegalStateException expected) { }
                try { parent.onFailure("child", "wrong context"); assert false : "Reject direct cross-thread use"; }
                catch (IllegalStateException expected) { }
                try { parent.run(() -> {}); assert false : "Active context must not be reused by a child"; }
                catch (IllegalStateException expected) { }
                var child = f.context(false);
                child.run(() -> { child.onFailure("child", "independent"); child.fail("child failure"); });
                return null;
            });
            Thread.ofVirtual().start(task);
            task.get(5, TimeUnit.SECONDS);
            assert Logging.context() == parent : "Child execution must not change the parent binding";
        });
        assert f.reports().size() == 1 : "An observed but handled child failure need not fail the parent";
    }
}

record ContextInputValidation(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        try { f.log().context(null); assert false : "Reject null configuration"; }
        catch (NullPointerException expected) { }
        var context = f.context(false);
        try { context.run(null); assert false : "Reject null work"; }
        catch (NullPointerException expected) { }
        context.run(() -> {
            try { context.onDebug("worker", null); assert false : "Reject null supplier even when debug disabled"; }
            catch (NullPointerException expected) { }
            try { context.onDebug(null, () -> "text"); assert false : "Reject null source even when debug disabled"; }
            catch (NullPointerException expected) { }
            try { context.immediately("worker", null); assert false : "Reject null message"; }
            catch (NullPointerException expected) { }
            context.onDebug("worker", () -> { throw new AssertionError("must not evaluate"); });
        });
        assert f.reports().isEmpty() : "Caught argument errors do not automatically mark failure";
    }
}

record LoggingWorker(String name) implements Logging {
    public String loggingName() { return name; }
}

record ObjectLogging(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(true);
        var first = new LoggingWorker("first"); var second = new LoggingWorker("second");
        context.run(() -> {
            first.logOnFailure("first evidence"); second.logOnFailure("second evidence");
            first.logOnDebug(() -> "debug"); second.logImmediately("immediate");
            Logging.context().fail("handled failure");
        });
        assert f.reports().size() == 1 && f.reports().getFirst().evidence().size() == 2 : "Objects contribute to the current context";
        assert f.reports().getFirst().evidence().getFirst().source().equals("first") : "Use overridden object name";
        assert f.entries().size() == 2 : "Default methods delegate output and lazy debug";
        assert new Logging() {}.loggingName().contains("ObjectLogging") : "Default name identifies implementing class";
        try { first.logImmediately("outside"); assert false : "Object logging without context fails explicitly"; }
        catch (IllegalStateException expected) { }
    }
}

record ReturningContext(Supplier<LoggingV03ProviderContract.Fixture> factory,
                        boolean explicit, boolean nullable) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        Object expected = nullable ? null : new Object();
        var calls = new AtomicInteger(); Thread caller = Thread.currentThread();
        Object result = context.call(() -> {
            assert Logging.context() == context && Thread.currentThread() == caller : "Call binds on the caller thread";
            calls.incrementAndGet();
            context.onFailure("worker", "value evidence");
            if (explicit) context.fail("marked before returning");
            return expected;
        });
        assert result == expected && calls.get() == 1 : "Return the exact nullable value from one invocation";
        assert f.reports().size() == (explicit ? 1 : 0) : "Publish explicit failure before returning; discard successful evidence";
        if (explicit) assert f.reports().getFirst().evidence().getFirst().message().equals("value evidence")
                : "Returning a value must not lose marked context evidence";
        try { Logging.context(); assert false : "Restore binding before returning the value"; }
        catch (IllegalStateException expectedFailure) { }
    }
}

record FailingCall(Supplier<LoggingV03ProviderContract.Fixture> factory, boolean error) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        Throwable original = error ? new AssertionError("call error") : new IOException("call checked");
        try {
            if (error) context.call(() -> { context.onFailure("worker", "before call error"); throw (AssertionError) original; });
            else checkedCall(context, (IOException) original);
            assert false : "Call failures must escape";
        } catch (IOException | AssertionError caught) {
            assert caught == original : "Preserve original checked exception or Error identity";
        }
        assert f.reports().size() == 1 && f.reports().getFirst().cause() == original
                : "Failed calls must publish one report with the original cause";
        try { Logging.context(); assert false : "Failed calls must restore bindings"; }
        catch (IllegalStateException expected) { }
    }
    private static String checkedCall(Context context, IOException failure) throws IOException {
        return context.call(() -> { context.onFailure("worker", "before checked call"); throw failure; });
    }
}

record NestedCall(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var outer = f.context(false); var inner = f.context(true);
        var original = new IOException("inner call");
        int result = outer.call(() -> {
            outer.onFailure("outer", "before inner");
            try { inner.call(() -> { throw original; }); assert false : "Inner failure must escape"; }
            catch (IOException caught) {
                assert caught == original && Logging.context() == outer : "Restore the outer binding after failed inner call";
            }
            return 42;
        });
        assert result == 42 && f.reports().size() == 1 && f.reports().getFirst().cause() == original
                : "Recovered inner call must not fail the returning parent";
    }
}

record CallLifecycle(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get(); var context = f.context(false);
        try { context.call(null); assert false : "Null call work must be rejected"; }
        catch (NullPointerException expected) { }
        int value = context.call(() -> {
            try { context.call(() -> 0); assert false : "Reject reentrant calls"; }
            catch (IllegalStateException expected) { }
            return 1;
        });
        assert value == 1 : "Null work must not consume the context";
        try { context.run(() -> {}); assert false : "Call consumes the same single-use lifecycle as run"; }
        catch (IllegalStateException expected) { }
        var ran = f.context(false); ran.run(() -> {});
        try { ran.call(() -> 0); assert false : "Run must prevent subsequent call reuse"; }
        catch (IllegalStateException expected) { }
    }
}
