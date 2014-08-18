package ir.ac.itrc.qqa.semantic.enums;

/**
 * Possible SRL arguments and modifiers
 * @author Ehsan Darrudi
 *
 */
public enum SrlArg
{
	// Main arguments
	ARG0,	// Proto-Agent
	ARG1,	// Proto-Patient
	ARG2,	// verb-specific
	ARG3,	// verb-specific
	ARG4,	// verb-specific
	ARG5,	// verb-specific

	// Modifiers
	COM,	// Commutative
	LOC,	// Locative: They [walk around the countryside], 
	DIR,	// Directional: They [walk along the road], No one wants the U.S. to pick up its marbles and go [home].
	GOL,	// Goal
	MNR,	// Manner: He works [well] with others
	TMP,	// Temporal: He was born [in 1980]	
	EXT,	// Extent (amount of change): He raised prices [more than she did], I like her [a lot], they raised the prices [by 150%]!
	REC,	// Reciprocal: himself, themselves, itself, together, each, other, jointly, both, etc.: If it were a good idea he would do it [himself] 
	PRP,	// Purpose
	CAU,	// Cause
	DIS,	// Discourse Markers: also, however, too, as well, but, and, as we've seen,	before, instead, on the other hand, for instance, etc.: 
	MOD,	// Modals: will, may, can, must, shall, might, should, could, would, going (to), have (to) and used (to)
	NEG,	// Negation: not, n't, never, no longer, etc.
	ADV,	// Adverbials (modify entire sentence): As opposed to ArgM-MNR, which modify the verb, ARGM-ADVs usually modify the	entire sentence.
	CND,	// Condition as [if you send me the money] I will pay you back as soon as possible.
	INS;		// Instrument, He was killed with a sledge hammer.
	
	public static SrlArg fromString(String str)
	{
		str = str.toUpperCase();
		
		if (str.startsWith("ARGM-"))
		{
			str = str.substring("ARGM-".length());
			
			// removing Brat's auto naming numbers such as 'ArgM-ADV2' when there are more than one argument type
			str = str.replaceAll("([0-9]+)$", "");
		}
		
		return valueOf(str.toUpperCase());
	}
}
