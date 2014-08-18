/* WordNetRelationType.java
 * Created on Jun 19, 2010
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.enums;

/**
 * Enumerates Wordnet relation types.
 * @author Ehsan
 *
 */
public enum WordNetRelationType
{
	SYN,
	/*
	It should be obvious that the most important semantic relation is 
	similarity of meaning. According to one definition (usually 
	attributed to Leibniz) two expressions are synonymous if the 
	substitution of one for the other never changes the truth value 
	of a sentence in which the substitution is made. By that 
	definition, true synonyms are rare, if they exist at all. 
	A weakened version of this definition would make synonymy 
	relative to a context: two expressions are synonymous in a 
	context C if the substitution of one for the other in C does not 
	change the truth value. But the important point is that theories 
	of lexical semantics do not depend on truth-functional conceptions 
	of synonymy; semantic similarity is sufficient. The relation is 
	symmetrical. If x is similar to y, then y is equally similar to x.
	*/
	ISA,
	/*
	- Hyponymy is transitive and asymmetrical: An x is a (kind of) y. 
	Since there is normally a single superordinat, this semantic 
	relation generates a hierarchical semantic structure where a 
	hyponym is said to be below its superordinate. The hyponym 
	inherits all the features of the more generic concept and adds 
	at least one feature that distinguishes it from its superordinate 
	and from other hyponyms of that superordinate.
	- A verb does not refer in the same way a noun does. 
	The "kind-of" relation corresponds to a "manner-of" relation 
	among the verbs. Troponymy is the most frequently found relation 
	among verbs: that is, most lexicalized verb concepts refer to an 
	action or event that constitutes a manner elaboration of another 
	activity or event.
	*/
	// TODO: implement instance_of for HPR
	SubstanceOf,
	PartOf,
	/*
	part-whole relation: 
	A y has an x (as a part) or an x is a part of y. 
	The meronymic relation is transitive and asymmetrical and can be used to construct a part hierarchy. 
	The concept of a part of a whole can be a part of a concept of the whole.
	*/
	// TODO: implement part_of for HPR
	MemberOf,
	/*
	A y has an x (as a part) or an x is a part of y. The meronymic 
	relation is transitive and asymmetrical and can be used to 
	construct a part hierarchy. The concept of a part of a whole 
	can be a part of a concept of the whole.
	*/
	CAUSES,
	/*
	Causation is a kind of entailment and, like the other entailment 
	relation, is asymmetrical. The troponyms of the verbs in a 
	causative-resultative pair inherit the relation of their 
	relative superordinate. 
	*/
	IMP, // Entailment
	/*
	ANY acceptable statement about part-relation among verbs always 
	involves the temporal relation between the activities that the 
	two verbs denote. One activity or event is part of another 
	activity or event only when it is part of, or a stage in, its 
	temporal realization. The activities can be simultaneous 
	(as with fatten and feed); one can include the other (as with 
	snore and sleep); or one can precede the other (try and succeed).
	Three kinds of lexical entailments include the different relations 
	illustrated by those pairs.
	*/
	SIM, // SimilarTo
	DIS, // Antonym
	/*
	The antonym of a word x is sometimes not-x, but not always. 
	Antonymy, which seems such a simple symmetrical relation, is at 
	least as complex as the other semantic relations. For example, 
	rich and poor are antonyms, but to say that someone is not rich 
	does not mean that they must be poor.
	*/
	ATTRIBUTE,
	
	//DerivationallyRelatedForm,
	//DomainOfSynsetCategory,
	//DomainOfSynsetRegion,
	//DomainOfSynsetUsage,
	//SeeAlso,
	//VerbGroup,
	//ParticipleOfVerb,
	//PertainsToNoun_DerivedFromAdjective,
	
	Useless
}

