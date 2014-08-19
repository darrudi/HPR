package ir.ac.itrc.qqa.semantic;

import java.util.ArrayList;
import ir.ac.itrc.qqa.semantic.enums.ExecutionMode;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleQuestion;
import ir.ac.itrc.qqa.semantic.reasoning.SemanticReasoner;


public class Sandbox 
{
	public static void main(String[] args) throws Exception 
	{
		checkSemanticReasoner();
	}
	
	
	public static void checkSemanticReasoner()
	{
		KnowledgeBase kb = new KnowledgeBase();
		kb.importKb("cache/kb/farsnet.txt");
		
		SemanticReasoner sr = new SemanticReasoner(kb, ExecutionMode.RELEASE);

		//Setting the max reasoning depth: important!
		sr.setMaxReasoningDepth(15);
		sr.setMaximumAnswers(1);
		
		PlausibleQuestion pq = new PlausibleQuestion();
		pq.argument = kb.addConcept("پسر بچه");
		pq.referent = kb.addConcept("نفر");
		pq.descriptor = KnowledgeBase.HPR_ISA;

//		pq.argument = kb.addConcept("پسر بچه");
//		pq.referent = kb.addConcept("بچه");
//		pq.descriptor = KnowledgeBase.HPR_ISA;
		
		System.out.print(pq.toString() + " ... ");
		
		ArrayList<PlausibleAnswer> answers = sr.answerQuestion(pq);
		
		System.out.println("done");
		
		System.out.println("Answers:");
		
		int count = 0;
		for (PlausibleAnswer answer: answers)
		{
			System.out.println(++count + ". " + answer.toString());
			
			ArrayList<String> justifications = answer.GetTechnicalJustifications();
			
			int countJustification = 0;
			for (String justification: justifications)
			{
				System.out.println("-------" + ++countJustification + "--------");
				System.out.println(justification);
			}
		}
		
		System.out.println("Summary:");
		System.out.println("\tInferences: " + sr.totalCalls);
		System.out.println("\tTime: " + sr.reasoningTime / 1000);
		System.out.println("\tThroughput: " + (sr.totalCalls / sr.reasoningTime) * 1000 + " inference/s");
	}	
}
