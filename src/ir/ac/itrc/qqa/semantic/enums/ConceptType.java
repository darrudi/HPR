/* ConceptType.java
 * Created on Jun 19, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates the conceptType of concepts (nodes) in the knowledge base
 * 
 * @author Ehsan Darrudi
 *
 */
public enum ConceptType
{
	STATEMENT,			// a descriptor (relation)
	EVENT,				// a plausible verb
	FRAME,				// a node containing a frame template
	CONCEPT_EXAMPLE,	// Farsnet|Wordnet example node
	CONCEPT_GLOSS,		// Farsnet|Wordnet gloss node
	CONCEPT_OTHER		// other non-statement nodes
}
