
package work.archaic.service.markdown.v01;

/**
 * Service interface for converting markup to HTML.
 * Supports a limited subset of markdown syntax.
 */
public interface Html {
    /**
     * Converts markdown text to HTML.
     * 
     * Supported markdown subset:
     * - Headers: # ## ### #### (h1-h4)
     * - Links: [text](url)
     * - Unordered lists: - item or * item
     * - Ordered lists: 1. item
     * - Inline code: `code`
     * - Code blocks: ```code``` or ````language code````
     * 
     * Any markdown syntax not in the supported subset will be left as plain text.
     * 
     * @param markdown the markdown source text
     * @return the converted HTML content (without html/head/body wrapper)
     * @throws IllegalArgumentException if markdown is null
     */
    String from(String markdown);
}
