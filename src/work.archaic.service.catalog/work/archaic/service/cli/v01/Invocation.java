package work.archaic.service.cli.v01;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record Invocation(
        Map<String, List<String>> options,
        List<String> positionals,
        PrintWriter out,
        PrintWriter err,
        Map<String, String> env,
        Path cwd,
        String appName,
        String commandName
) {
    public Invocation {
        options = Map.copyOf(options);
        positionals = List.copyOf(positionals);
        env = Map.copyOf(env);
    }

    public boolean has(String longName) {
        return options.containsKey(longName);
    }

    public String optOne(String longName, String def) {
        var v = options.get(longName);
        return (v == null || v.isEmpty()) ? def : v.getFirst();
    }

    public List<String> optMany(String longName) {
        return options.getOrDefault(longName, List.of());
    }
}
