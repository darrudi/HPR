package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates possible HPR dependency relation types.
 * @author Ehsan
 *
 */
public enum DependencyType
{
	POSITIVE,	// -(+)-> dependency
	NEGATIVE,	// -(-)-> dependency
	MARKED,		// + or -
	UNMARKED,	// -->
	ANY			// unimportant
}

