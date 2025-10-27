package work.archaic.service.cli.v01;

import java.util.List;

public record Parameter(List<String> names, Arity arity, String metavar, String help) {
    public enum Arity { FLAG, SINGLE, REPEATABLE }

    public Parameter {
        names = List.copyOf(names);
    }
}
