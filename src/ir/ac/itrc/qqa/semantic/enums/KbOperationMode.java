package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates possible knowledgebase operation modes.
 * @author Ehsan
 *
 */
public enum KbOperationMode
{
	STATIC,	 	// normal operation: nodes are loaded once and remain in the _kb 
	DYNAMIC 	// new nodes are marked as temporary. later these new nodes can be removed without affecting normal nodes.
}
