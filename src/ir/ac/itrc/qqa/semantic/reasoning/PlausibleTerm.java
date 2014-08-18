/* PlausibleTerm.java
 * Created on May 24, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.reasoning;

import ir.ac.itrc.qqa.semantic.kb.CertaintyParameters;
import ir.ac.itrc.qqa.semantic.kb.Node;


/**
 * 
 * Represents a Plausible Term.
 * Plausible Terms in HPR are actually referent-less statement. They are represented as DESCRIPTOR(argument). 
 * This way, a plausible statement is reduced to: PLTerm={referent} 
 * Each term has a certainty parameter attached to it.
 *
 * @author Ehsan Darrudi
 */

public class PlausibleTerm extends Node
{
	/** Relation conceptType of the term (and statement if it's going to be one) */
	public Node relationType;

	/** certainty parameters attached to this term */
	public CertaintyParameters parameters;

	/** the argument of this term */
	public Node argument;

	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	/**
	 * the sole constructor
	 */
	protected PlausibleTerm(Node DescriptorType, CertaintyParameters Params, Node SourceIn)
	{
		super(DescriptorType);
		
		argument = SourceIn;
			
		relationType = DescriptorType;
			
		parameters = new CertaintyParameters(Params);
	}
}

