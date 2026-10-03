package work.archaic.service.catalog.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import work.archaic.service.logging.v03.*;
import work.archaic.service.test.v02.*;

/** Reusable conformance cases. Each factory call must return an independent provider,
 * thread-safe observed lists, capacity two and field limit 32. */
public final class LoggingV03ProviderContract {
    private LoggingV03ProviderContract() {}
    public record Fixture(Log log, List<Entry> entries, List<FailureReport> reports) {}
    public static void cases(Collection<TestCase> cases, Supplier<Fixture> factory) {
        cases.add(new SuccessfulTrail(factory));
        cases.add(new EscapingFailure(factory, false));
        cases.add(new EscapingFailure(factory, true));
        cases.add(new ExplicitFailure(factory));
        cases.add(new NestedTrail(factory, false));
        cases.add(new NestedTrail(factory, true));
        cases.add(new DebugOutput(factory));
        cases.add(new TrailBounds(factory));
        cases.add(new OutsideTrail(factory));
        cases.add(new ConcurrentTrails(factory));
        cases.add(new NoChildInheritance(factory));
        cases.add(new NullInputs(factory));
    }
}

record SuccessfulTrail(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        Thread caller = Thread.currentThread();
        f.log().trail(() -> {
            assert Thread.currentThread() == caller : "Trail must execute on its calling thread";
            f.log().onFailure("worker", "temporary");
            f.log().immediately("worker", "visible");
        });
        assert f.entries().size() == 1 : "Immediate entry must be published";
        assert f.reports().isEmpty() : "Success must discard its evidence";
        f.log().trail(() -> f.log().failure("worker", "next failure"));
        assert f.reports().getFirst().evidence().isEmpty() : "Success evidence must not leak into later trails";
    }
}

record EscapingFailure(Supplier<LoggingV03ProviderContract.Fixture> factory,
                       boolean error) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        var checked = new IOException("original");
        var fatal = new AssertionError("original error");
        try {
            f.log().trail(() -> {
                f.log().onFailure("worker", "before");
                if (error) throw fatal;
                throw checked;
            });
            assert false : "Escaping failure must be rethrown";
        } catch (IOException caught) {
            assert !error && caught == checked : "Preserve checked exception identity";
        } catch (AssertionError caught) {
            assert error && caught == fatal : "Preserve error identity";
        }
        assert f.reports().size() == 1 : "Publish escaping failure exactly once";
        var report = f.reports().getFirst();
        assert report.cause() == (error ? fatal : checked) : "Report must retain the original cause";
        assert report.evidence().getFirst().message().equals("before") : "Retain preceding evidence";
        f.log().trail(() -> {});
        assert f.reports().size() == 1 : "Cleanup must restore an empty context";
        try { report.evidence().clear(); assert false : "Evidence must be immutable"; }
        catch (UnsupportedOperationException expected) { }
    }
}

record ExplicitFailure(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        f.log().trail(() -> {
            f.log().failure("worker", "first reason");
            f.log().failure("worker", "later reason");
            assert f.reports().isEmpty() : "Explicit failure publishes at the outer boundary";
            f.log().onFailure("worker", "after marking failure");
        });
        var report = f.reports().getFirst();
        assert f.reports().size() == 1 : "Multiple marks must publish one report";
        assert report.cause() == null : "Explicit failure does not invent an exception";
        assert report.explicitFailure().message().equals("first reason") : "Retain first explicit reason";
        assert report.evidence().size() == 1 : "Include evidence submitted after failure was marked";
    }
}

record NestedTrail(Supplier<LoggingV03ProviderContract.Fixture> factory,
                   boolean recover) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        var cause = new IOException("inner");
        try {
            f.log().trail(() -> {
                f.log().onFailure("outer", "outer");
                try {
                    f.log().trail(() -> {
                        f.log().onFailure("inner", "inner");
                        throw cause;
                    });
                } catch (IOException caught) {
                    assert f.reports().isEmpty() : "Inner boundary must not publish independently";
                    if (!recover) throw caught;
                }
            });
        } catch (IOException caught) {
            assert !recover && caught == cause : "Outer boundary must rethrow original inner failure";
        }
        assert f.reports().size() == (recover ? 0 : 1) : "Caught and recovered exceptions are successful";
        if (!recover) {
            var evidence = f.reports().getFirst().evidence();
            assert evidence.size() == 2 : "Nested execution shares outer evidence";
            assert evidence.getFirst().source().equals("outer") : "Capture source at submission";
            assert evidence.getLast().source().equals("inner") : "Preserve evidence order";
        }
    }
}

record DebugOutput(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) {
        var f = factory.get();
        assert !f.log().debug() : "Debug must initially be disabled";
        f.log().onDebug("worker", "hidden");
        assert f.entries().isEmpty() : "Disabled debug must discard output";
        f.log().debug(true);
        f.log().onDebug("worker", "visible");
        f.log().debug(false);
        f.log().onDebug("worker", "hidden again");
        assert f.entries().size() == 1 : "Only enabled debug entries are published";
        assert f.entries().getFirst().timestamp() != null : "Capture a timestamp";
    }
}

record TrailBounds(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        String longMessage = "x".repeat(30) + "😀" + "x".repeat(10);
        f.log().trail(() -> {
            f.log().onFailure("worker", "evicted");
            f.log().onFailure("worker", "retained");
            f.log().onFailure("s".repeat(100), longMessage);
            f.log().failure("worker", "failure");
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

record OutsideTrail(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) {
        var f = factory.get();
        try { f.log().onFailure("worker", "lost"); assert false : "Evidence outside a trail must fail explicitly"; }
        catch (IllegalStateException expected) { }
        try { f.log().failure("worker", "lost"); assert false : "Failure outside a trail must fail explicitly"; }
        catch (IllegalStateException expected) { }
        assert f.reports().isEmpty() : "No synthetic report outside a trail";
    }
}

record ConcurrentTrails(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        var ready = new CountDownLatch(2);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (String name : List.of("one", "two")) {
                futures.add(executor.submit(() -> {
                    f.log().trail(() -> {
                        f.log().onFailure(name, name);
                        ready.countDown();
                        assert ready.await(5, TimeUnit.SECONDS) : "Both independent trails must overlap";
                        f.log().failure(name, name);
                    });
                    return null;
                }));
            }
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        }
        assert f.reports().size() == 2 : "Each independent execution must produce its own report";
        for (var report : f.reports()) {
            assert report.evidence().size() == 1 : "Concurrent trails must not share evidence";
            assert report.evidence().getFirst().source().equals(report.explicitFailure().source()) : "Associate evidence with its own failure";
        }
    }
}

record NoChildInheritance(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        f.log().trail(() -> {
            var task = new java.util.concurrent.FutureTask<Void>(() -> {
                try { f.log().onFailure("child", "wrong context"); assert false : "Ordinary child threads must not inherit a trail"; }
                catch (IllegalStateException expected) { }
                return null;
            });
            Thread.ofVirtual().start(task);
            task.get(5, TimeUnit.SECONDS);
        });
        assert f.reports().isEmpty() : "Child test must not fail parent trail";
    }
}

record NullInputs(Supplier<LoggingV03ProviderContract.Fixture> factory) implements TestCase {
    public void run(TestTrail test) throws Exception {
        var f = factory.get();
        try { f.log().trail(null); assert false : "Reject null work"; }
        catch (NullPointerException expected) { }
        try { f.log().immediately("source", null); assert false : "Reject null messages"; }
        catch (NullPointerException expected) { }
        try { f.log().onDebug(null, "message"); assert false : "Validate inputs even when debug is disabled"; }
        catch (NullPointerException expected) { }
        f.log().trail(() -> {});
        assert f.reports().isEmpty() : "Invalid call outside a trail must not leave a context";
    }
}
