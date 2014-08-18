package ir.ac.itrc.qqa.semantic.reasoning;

/**
 * Implements a plausible question.
 * 
 * @author Ehsan Darrudi
 */

/*
import edu.stanford.nlp.trees.Tree;
*/
import ir.ac.itrc.qqa.semantic.enums.RelationType;
import ir.ac.itrc.qqa.semantic.kb.CertaintyParameters;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.kb.Node;

/**
 * 
 * Represents a Plausible Question which is fed to the Reasoning Engine or passed between HPR inferences.
 * Implements a single Plausible Question. A plausible statement in composed on 5 nodes:
 * <ul>
 * <li>argument</li>
 * <li>DESCRIPTOR</li>
 * <li>referent</li>
 * <li>cxLocation</li>
 * <li>cxTime</li>
 * <p>
 * which represents "DESCRIPTOR(argument)={referent} CX:TIME={cxTime} CX:LOCATION={cxLocation}"
 * <br> A plausible question may have a missing node. So there are 3 kind of plausible questions:
 * <br> 1. DESCRIPTOR(argument)={?}
 * <br> 2. DESCRIPTOR(?)={referent}
 * <br> 3. DESCRIPTOR(argument)={referent}?
 * 
 * cxLocation and cxTime can also be null so we have some additional question types:
 * <br> 4. DESCRIPTOR(argument)={referent} CX:TIME={?}
 * <br> 5. DESCRIPTOR(argument)={referent} CX:LOCATION={?}
 * <br> 6. DESCRIPTOR(argument)={referent} CX:TIME={cxTime}?
 * <br> 7. DESCRIPTOR(argument)={referent} CX:LOCATION={cxLocation}?
 * <p>
 * For question types 1,2,4,5 above the system will try to replace question mark (?) with a concept. 
 * For types 3,6,7 the reasoning engine answers YES or NO.
 * 
 * @author Ehsan Darrudi
 */
public class PlausibleQuestion implements Comparable<Object>
{
	// original 
	
	public Node descriptor;
	public Node argument;
	public Node referent;
	public Node secondArgument;
	public Node secondReferent;
	public Node cxTime;
	public Node cxLocation;
	
	// PropBank
	
	public float DescriptorAmbiguity = 0;
	
	public float ArgumentAmbiguity = 0;
	public float SecondArgumentAmbiguity = 0;

	public float ReferentAmbiguity = 0;
	public float SecondReferentAmbiguity = 0;

	public float Priority;

	public boolean IsMultiArgument = false;
	public boolean IsMultiReferent = false;

	public String TaggedQuestion;
	
	public RelationType type = RelationType.PROPERTY; // default
	
	public CertaintyParameters parameters = new CertaintyParameters();
	
	public String question = "";
	/*public Tree parse = null;*/
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	public PlausibleQuestion()
	{
		descriptor = null;
		argument = null;
		referent = null;
		
		cxTime = KnowledgeBase.HPR_ANY;
		cxLocation = KnowledgeBase.HPR_ANY;
	}

	public PlausibleQuestion(Node DescriptorIn, Node ArgumentIn, Node ReferentIn)
	{
		descriptor = DescriptorIn;
		argument = ArgumentIn;
		referent = ReferentIn;
        
		cxTime = KnowledgeBase.HPR_ANY;
		cxLocation = KnowledgeBase.HPR_ANY;

		secondArgument = null;
		secondReferent = null;

		DescriptorAmbiguity = 0;
		ArgumentAmbiguity = 0;
		ReferentAmbiguity = 0;

		SecondArgumentAmbiguity = 0;
		SecondReferentAmbiguity = 0;
		
		Priority = 0;
	}
	
	@Override
	public String toString()
	{
		String Des, Arg, Ref;
		String cx;
		
		if (descriptor == null)
		{
			Des = "?";
		}
		else
		{
			Des = descriptor.getName();
		}

		if (argument == null)
		{
			Arg = "?";
		}
		else
		{
			Arg = argument.getName();
		}

		if (IsMultiArgument)
		{
			Arg += ", ";
			if (secondArgument == null)
			{
				Arg += "?";
			}
			else
			{
				Arg += secondArgument.getName();
			}	
		}

		if (referent == null)
		{
			Ref = "?";
		}
		else
		{
			Ref = referent.getName();
		}

		if (IsMultiReferent)
		{
			Ref += ", ";
			if (secondReferent == null)
			{
				Ref += "?";
			}
			else
			{
				Ref += secondReferent.getName();
			}	
		}
		
		cx = "";
		
		if (cxTime != KnowledgeBase.HPR_ANY)
		{
			cx += " CX:TIME = {";
			
			if (cxTime != null)
			{
				cx += cxTime.getName();
			}
			else
			{
				cx += "?";
			}
			
			cx += "}";
		}
		
		if (cxLocation != KnowledgeBase.HPR_ANY)
		{
			cx += " CX:LOCATION = {";
			
			if (cxLocation != null)
			{
				cx += cxLocation.getName();
			}
			else
			{
				cx += "?";
			}
			
			cx += "}";
		}

		return Des + "(" + Arg + ")={" + Ref + "}" + cx;
	}
	
	@Override
	public int compareTo(Object obj)
	{
		if (this.Priority > ((PlausibleQuestion)obj).Priority)
		{
			return -1;
		}
		else if (this.Priority <((PlausibleQuestion)obj).Priority)
		{
			return +1;
		}
		
		return 0;
	}
	
	@Override
	public PlausibleQuestion clone()
	{
		PlausibleQuestion pq = new PlausibleQuestion();
		
		pq.descriptor = descriptor;
		pq.argument = argument;
		pq.referent = referent;
		
		pq.cxTime = cxTime;
		pq.cxLocation = cxLocation;
		
		pq.secondArgument = secondArgument;
		pq.secondReferent = secondReferent;
		
		pq.DescriptorAmbiguity = DescriptorAmbiguity;
		pq.ArgumentAmbiguity = ArgumentAmbiguity;
		pq.SecondArgumentAmbiguity = SecondArgumentAmbiguity;
		pq.ReferentAmbiguity = ReferentAmbiguity;
		pq.SecondReferentAmbiguity = SecondReferentAmbiguity;

		pq.Priority = Priority;

		pq.IsMultiArgument = IsMultiArgument;
		pq.IsMultiReferent = IsMultiReferent;

		pq.TaggedQuestion = TaggedQuestion;
		
		pq.type = type;
		
		pq.parameters = parameters;
		
		return pq;
	}
}