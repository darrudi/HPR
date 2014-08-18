/* ReasoningDirection.java
 * Created on Jun 19, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.enums;

/**
 * For a hierarchical HPR inference defines the reasoning direction
 * @author Ehsan
 *
 */
public enum ReasoningDirection
{
	UP,				// it is a generalization inference
	DOWN,			// it is a specification inference
	SIDEWAY,		// it is a similarity inference
	ASKANCE,		// it is a dissimilarity inference
	UNDETERMINED	// not known
}

