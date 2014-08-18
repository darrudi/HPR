package ir.ac.itrc.qqa.semantic.reasoning;

import ir.ac.itrc.qqa.semantic.enums.ConditionalType;
import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.kb.CertaintyParameters;
import ir.ac.itrc.qqa.semantic.kb.Node;

/**
 * 
 * Represents a Plausible Statement
 * This class represents a plausible statement in the form: DESCRIPTOR(argument)={referent}. 
 * Because we represent 'DESCRIPTOR(argument)' with a Plausible Term the statement is reduced to PlausibleTerm={referent). 
 * So we extend PlausibleTerm with a referent node to achieve a statement.  
 * 
 * @author Ehsan Darrudi
 * @version 1.0, Jun 19, 2010
 */

public class PlausibleStatement extends PlausibleTerm implements Cloneable
{
	/** statement conceptType **/
	public ConditionalType conditionalType;

	/** referent of this statement */
	public Node referent;

	/** holds the polarity of this statement */
	public boolean isStatementNegative = false;

	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/** 
	 * getter for <code>isStatementNegative</code> 
	*/
	public boolean IsNegative()
	{
		return isStatementNegative; 
	}
	
	/**
	 * It is the sole constructor of this class. It calls the Node's constructor with a unique name for this relation.
	 * 
	 * @param DescriptorType conceptType of the descriptor
	 * @param Params Certainty parameters associated with the statement
	 * @param SourceIn source node
	 * @param DestinationIn target node
	 * @param StatType conceptType of the statement
	 */
	public PlausibleStatement(Node DescriptorType, CertaintyParameters Params, Node SourceIn, Node DestinationIn, ConditionalType StatType)
	{
		super(DescriptorType, Params, SourceIn);
		
		referent = DestinationIn;
		
		conditionalType = StatType;
	}

	/**
	 * updates the properties of an existing statement
	 * @param CP certainty parameters
	 * @param newType new statement conceptType
	 */
	public void updateStatementProperties(CertaintyParameters CP, ConditionalType newType, SourceType source)
	{
		if (CP != null)
		{
			parameters = new CertaintyParameters(CP);
		}

		if (conditionalType == ConditionalType.ANTECEDENT && newType == ConditionalType.CONSEQUENT || conditionalType == ConditionalType.CONSEQUENT && newType == ConditionalType.ANTECEDENT)
		{
			conditionalType = ConditionalType.ANTECEDENT_AND_CONSEQUENT;
		}
		else if (conditionalType == ConditionalType.NOT_CONDITIONAL)
		{
			conditionalType = newType;
		}
		else if (newType == ConditionalType.ANTECEDENT_AND_CONSEQUENT)
		{
			conditionalType = ConditionalType.ANTECEDENT_AND_CONSEQUENT;
		}
		
		this.setSourceType(source); 
	}

	/**
	 * determines if the statement id an antecedent for an IMP relation.
	 * @return true/false
	 */
	public boolean IsAntecedentStatement()
	{
		if (conditionalType == ConditionalType.ANTECEDENT || conditionalType == ConditionalType.ANTECEDENT_AND_CONSEQUENT)
		{
			return true;
		}

		return false;
	}
	
	/**
	 * determines if the statement id an consequent for an IMP relation
	 * @return true/false
	 */
	public boolean IsConsequentStatement()
	{
		if (conditionalType == ConditionalType.CONSEQUENT || conditionalType == ConditionalType.ANTECEDENT_AND_CONSEQUENT)
		{
			return true;
		}

		return false;
	}
	
	/**
	 * unbinds the statement form argument and referent so the garbage collector can free the memory used by it.
	 */
	@Override
	public int unbindRelations()
	{
		argument.removeOutLink(this);
		argument = null;
		
		referent.removeInLink(this);
		referent = null;
		
		relationType = null;
		
		parameters = null;
		
		return 1;
	}
	
	/**
	 * changes the argument of this statement
	 * @param node the concept which going to be the new argument 
	 */
	public void changeArgument(Node node)
	{
		argument.removeOutLink(this);
		argument = node;
		argument.addOutLink(this.referent, this);
		
		referent.removeInLink(this);
		referent.addInLink(node, this);
	}
	
	/**
	 * changes the reference of this statement
	 * @param node the concept which is going to be the new referent
	 */
	public void changeReferent(Node node)
	{
		referent.removeInLink(this);		
		referent = node;
		referent.addInLink(this.argument, this);
		
		argument.removeOutLink(this);
		argument.addOutLink(node, this);
	}
	
	@Override
	public String toString()
	{	
		// TODO: for JUNG
		
		return "*" + relationType.getName();		
		
		/*
		String out = "";
		String Sign = "=";

		if (isStatementNegative)
		{
			Sign = "≠";
		}
			
		out += relationType + "(" + argument.name + ")" + Sign + "{" + referent.name + "}";
		
		//-----------
		
		ArrayList<PlausibleAnswer> cxs;
		ArrayList<PlausibleAnswer> cxTimes;
		ArrayList<PlausibleAnswer> cxLocations;
		
		cxs = findTargetNodes(KnowledgeBase.HPR_CX);
		cxTimes = findTargetNodes(KnowledgeBase.HPR_CXTIME);
		cxLocations = findTargetNodes(KnowledgeBase.HPR_CXLOCATION);
		
		for(PlausibleAnswer pa: cxs)
		{
			out += " CX={" + pa.answer.name + "}"; 
		}
		
		for(PlausibleAnswer pa: cxTimes)
		{
			out += " CX:TIME={" + pa.answer.name + "}"; 
		}
		
		for(PlausibleAnswer pa: cxLocations)
		{
			out += " CX:LOCATION={" + pa.answer.name + "}"; 
		}
				
		return out;
		*/
	}
}