package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates possible graph layouts used to depict concepts and relations
 * @author Ehsan Darrudi
 *
 */
public enum GraphLayouts 
{
	KK,
	CIRCLE,
	FR,
	ISO,
	SPRING1,
	SPRING2;
	
	public GraphLayouts next() 
	{
		return GraphLayouts.values()[(this.ordinal() + 1) % GraphLayouts.values().length];
	}
}