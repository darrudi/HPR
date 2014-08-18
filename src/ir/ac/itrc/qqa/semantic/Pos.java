package ir.ac.itrc.qqa.semantic;

import ir.ac.itrc.qqa.semantic.enums.PosCoarse;
import ir.ac.itrc.qqa.semantic.enums.PosFine;
import ir.ac.itrc.qqa.semantic.enums.PosNumber;
import ir.ac.itrc.qqa.semantic.enums.PosPerson;
import ir.ac.itrc.qqa.semantic.enums.PosPolarity;
import ir.ac.itrc.qqa.semantic.enums.PosTense;

public class Pos 
{
	public PosCoarse coarse;
	public PosFine fine 		= PosFine.NOT_APPLICABLE;
	public PosTense tense 		= PosTense.NOT_APPLICABLE;
	public PosPerson person 	= PosPerson.NOT_APPLICABLE;
	public PosNumber number 	= PosNumber.NOT_APPLICABLE;
	public PosPolarity polarity = PosPolarity.NOT_APPLICABLE;
	
	//=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~
	
	@Override
	public String toString() 
	{
		String out = coarse.toString();
		
		if (fine != PosFine.NOT_APPLICABLE)
			out += "_" + fine.toString();
		
		if (tense != PosTense.NOT_APPLICABLE)
			out += "_" + tense.toString();
		
		if (person != PosPerson.NOT_APPLICABLE)
			out += "_" + person.toString();
		
		if (number != PosNumber.NOT_APPLICABLE)
			out += "_" + number.toString();
		
		if (polarity != PosPolarity.NOT_APPLICABLE)
			out += "_" + polarity.toString();
		
		return out;
	}
		
	public String toStringMinimal() 
	{
		String out = coarse.toString();
		
		if (fine != PosFine.NOT_APPLICABLE)
			out += "_" + fine.toString();
		
		if (polarity != PosPolarity.NOT_APPLICABLE)
			out += "_" + polarity.toString();
		
		return out;
	}


	@Override
	public boolean equals(Object obj)
	{
		Pos in = (Pos)obj;
		
		if (in.coarse != this.coarse)
			return false;
		
		if ((in.fine != PosFine.NOT_APPLICABLE && this.fine != PosFine.NOT_APPLICABLE) && in.fine != this.fine)
			return false;
		
		if ((in.tense != PosTense.NOT_APPLICABLE && this.tense != PosTense.NOT_APPLICABLE) && in.tense != this.tense)
			return false;
		
		if ((in.person != PosPerson.NOT_APPLICABLE && this.person != PosPerson.NOT_APPLICABLE) && in.person != this.person)
			return false;
		
		if ((in.number != PosNumber.NOT_APPLICABLE && this.number != PosNumber.NOT_APPLICABLE) && in.number != this.number)
			return false;
		
		if ((in.polarity != PosPolarity.NOT_APPLICABLE && this.polarity != PosPolarity.NOT_APPLICABLE) && in.polarity != this.polarity)
			return false;
		
		return true;
	}
}
