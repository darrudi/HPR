/* POS.java
 * Created on Jun 19, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates valid part of speech (wide) categories
 * @author Ehsan
 *
 */
public enum POS
{
	NOUN,
	VERB,
	ADJECTIVE,
	SETELLITE_ADJECTIVE,
	ADVERB,
	ANY,
	UNKNOWN;					// other categories such as CONJ, PREP, etc
	
	public static POS fromString(String posTag){
		if (posTag != null)
			for (POS pos : POS.values()) 
				if (posTag.equalsIgnoreCase(pos.name())) 
					return pos;
       return null;
	}
}

