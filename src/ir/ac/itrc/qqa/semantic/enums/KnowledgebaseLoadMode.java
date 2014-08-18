package ir.ac.itrc.qqa.semantic.enums;

/**
 * Defines the knowledgebase operation mode. 
 * @author Ehsan Darrudi
 *
 */
public enum KnowledgebaseLoadMode 
{
	NORMAL,		// normal mode
	IMPORT		// we are importing a knowledgebase from file. in this state some checks are not done as we suppose the written knowledgebase is already well-formed
}
