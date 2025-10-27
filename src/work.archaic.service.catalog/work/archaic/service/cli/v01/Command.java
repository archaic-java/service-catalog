package work.archaic.service.cli.v01;

import java.util.List;

public interface Command {
    String name();
    String description();
    default List<Parameter> parameters() { return List.of(); }
    int run(Invocation in) throws CommandException;
}
