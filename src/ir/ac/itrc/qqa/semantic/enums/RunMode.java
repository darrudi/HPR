package ir.ac.itrc.qqa.semantic.enums;

public enum RunMode
{
	TEST,		// batch test: reasoner will produce only the maximum of topK answers. 
	SERVICE		// normal execution mode: reasoner will produce as many answers as it likes.
}
