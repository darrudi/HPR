/* ConditionalType.java
 * Created on Jun 19, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates conditional types for HPR statements
 * 
 * @author Ehsan Darrudi
 *
 */
public enum ConditionalType
{
	NOT_CONDITIONAL,			// normal statements
	ANTECEDENT,					// if X ==> ...
	CONSEQUENT,					// if ... => X
	ANTECEDENT_AND_CONSEQUENT 	// for chained rules: if ... => X, if X ==> ..
}

