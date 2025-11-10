package work.archaic.service.html.sitegenerator.v01;

/**
 * Service interface for static site generation.
 * Implementations should generate a complete static website
 * from markdown source files.
 */
public interface Generator {
    /**
     * Generates the static website.
     * Must be called from the project root directory.
     * 
     * @throws GeneratorException if any error occurs during generation
     */
    void generate() throws GeneratorException;
}
