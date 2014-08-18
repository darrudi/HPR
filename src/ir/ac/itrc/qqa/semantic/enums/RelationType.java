package ir.ac.itrc.qqa.semantic.enums;

/**
 * Defines whether this piece of knowledge is a statement or a verb (SRL)
 * @author Ehsan
 *
 */
public enum RelationType
{
	PROPERTY,	// represents static knowledge, e.g. Color(Book)={Yellow}, IS(Sky)={Blue}, ...
	VERB		// represents verb-based statements, e.g. Loves(Ehsan)={Maryam}, ... 
}
