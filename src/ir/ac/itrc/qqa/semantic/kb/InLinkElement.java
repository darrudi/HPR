package ir.ac.itrc.qqa.semantic.kb;

import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;


/**
 * Implements a single InLinkElement element. together these elements constitute a link-list.
 * each node has an InLinkElement element that retains all the relations that have the node as their referents. 
 * 
 * @author Ehsan Darrudi
 */
public class InLinkElement implements Cloneable
{
	/** Reference to the destination node */
	protected Node sourceNode = null;

	/** Contains relation conceptType + certainty parameters */
	protected PlausibleStatement relation;

	/** a source to the last element in the link-list */
	protected InLinkElement previousInLinkElement = null;

	/**
	 * the sole constructor
	 * 
	 * @param pr Input plausible relation
	 */
	protected InLinkElement(PlausibleStatement pr)
	{
		relation = pr;
	}
	
	/**
	 * deep clones the object
	 */
	public InLinkElement clone()
	{
		InLinkElement clone = new InLinkElement(this.relation);
		
		clone.sourceNode = this.sourceNode;  
		clone.previousInLinkElement = this.previousInLinkElement.clone();
		
		return clone;		
	}
	
}

