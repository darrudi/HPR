package ir.ac.itrc.qqa.semantic.enums;

/**
 * When searching in the Knowledge Explorer defines what kind of matches are desirable
 * @author Ehsan Darrudi
 *
 */
public enum StringMatch
{
	PREFIX,		// only prefixes 
	SUBSTRING,	// anywhere in the string
	EXACT,		// whole words only
	SUBNUMBER,	// used to search for synset names
	WHOLE_WORD	// only whole words
}
