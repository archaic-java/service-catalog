
package work.archaic.service.html.sitegenerator.v01;

/**
 * Checked exception thrown by the Generator service when
 * any error occurs during site generation.
 */
public class GeneratorException extends Exception {
    public GeneratorException(String message) {
        super(message);
    }
    
    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GeneratorException(Throwable cause) {
        super(cause);
    }
}
