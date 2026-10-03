package work.archaic.service.catalog.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import work.archaic.service.logging.v03.*;
import work.archaic.service.logging.v03.Configuration;

/** Standard renderer checks independent of a logging provider. */
public final class TextOutputContractTest {
    public static void main(String[] args) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var output = new TextOutput(stream);
        var evidence = new Entry(Instant.EPOCH, "worker\nname", "value\\\r\nnext");
        output.entry(evidence);
        assert bytes.toString(StandardCharsets.UTF_8).strip().equals("1970-01-01T00:00:00Z worker\\nname value\\\\\\r\\nnext")
                : "Standard entries must preserve timestamp and escape fields onto one line";
        bytes.reset();
        output.failure(new FailureReport(List.of(evidence), 3,
                new Entry(Instant.EPOCH, "reason", "marked"), new java.io.IOException("original")));
        String report = bytes.toString(StandardCharsets.UTF_8);
        assert report.startsWith("--- failed logging context ---") && report.contains("[3 earlier entries dropped]")
                && report.contains("Failure: 1970-01-01T00:00:00Z reason marked")
                && report.contains("java.io.IOException: original") && report.strip().endsWith("--- end context ---")
                : "Render loss count, evidence, explicit reason and original throwable";
        var configuration = Configuration.text(true, stream);
        assert configuration.debug() && configuration.capacity() == 256 && configuration.fieldLimit() == 2048
                : "Stream convenience must use documented retention defaults";
        bytes.reset(); configuration.entries().accept(evidence);
        assert !bytes.toString(StandardCharsets.UTF_8).isEmpty() : "The selected stream must receive configured output";
        try { Configuration.text(false, null); assert false : "Reject null output destinations"; }
        catch (NullPointerException expected) { }
        try { output.failure(null); assert false : "Reject null reports"; }
        catch (NullPointerException expected) { }
        try { output.entry(null); assert false : "Reject null entries"; }
        catch (NullPointerException expected) { }

        bytes.reset();
        var other = new TextOutput(stream);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int task = 0; task < 4; task++) {
                int number = task;
                futures.add(workers.submit(() -> {
                    for (int i = 0; i < 25; i++) {
                        var entry = new Entry(Instant.EPOCH, "worker", "report " + number + ":" + i);
                        var renderer = number % 2 == 0 ? output : other;
                        renderer.failure(new FailureReport(List.of(entry, entry), 0, entry, null));
                        renderer.entry(entry);
                    }
                }));
            }
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        }
        var lines = bytes.toString(StandardCharsets.UTF_8).lines().toList();
        int reports = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).equals("--- failed logging context ---")) {
                assert lines.get(i).startsWith("1970-01-01T00:00:00Z worker report ") : "Only standalone entries may occur outside reports";
                continue;
            }
            reports++;
            String first = lines.get(++i);
            assert lines.get(++i).equals(first) && lines.get(++i).equals("Failure: " + first)
                    && lines.get(++i).equals("--- end context ---")
                    : "Separate renderer instances sharing a stream must not interleave entries or reports";
        }
        assert reports == 100 : "Render every concurrent report exactly once";
        stream.println("still open");
        assert bytes.toString(StandardCharsets.UTF_8).endsWith("still open" + System.lineSeparator())
                : "Renderer must leave the caller's stream open";
        System.out.println("Logging v03 text output: checks passed");
    }
}
