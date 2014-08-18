package ir.ac.itrc.qqa.semantic.kb;

import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;


/**
 * implements a single OutLinkElement element. together these elements constitute a link-list.
 * each node has an OutLinkElement element that retains all the relations that have the node as their argument.
 * @author root
 *
 */
public class OutLinkElement
{
	/** The source to destination node. */
	protected Node destinationNode = null;

	/** relation conceptType + certainty parameters. */
	protected PlausibleStatement relation;

	/** a source to the last element in the link-list */
	protected OutLinkElement previousOutLinkElement;

	/**
	 * the sole constructor
	 * @param pr The input plausible relation
	 */
	protected OutLinkElement(PlausibleStatement pr)
	{
		relation = pr;
	}
	
	/**
	 * deep clones the object
	 */
	public OutLinkElement clone()
	{
		OutLinkElement clone = new OutLinkElement(this.relation);
		
		clone.destinationNode = this.destinationNode;  
		clone.previousOutLinkElement = this.previousOutLinkElement.clone();
		
		return clone;		
	}
}

