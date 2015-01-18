package ir.ac.itrc.qqa.semantic.enums;

public enum DependencyRelationType 
{
	ACL,
	ADVC,
	ADVEZ,
	ADVPP,
	ADVRB,
	AJCONJ,
	AJPP,
	AJUCL,
	APOSTMOD,
	APP,
	APREMOD,
	AVCL,
	AVCONJ,
	COMPPP,
	ENC,
	MESU,
	MOS,
	MOZ,
	NADV,
	NCL,
	NCONJ,
	NE,
	NEZ,
	NPOSTMOD,
	NPP,
	NPREMOD,
	NPRT,
	NVE,
	OBJ,
	OBJ2,
	PARCL,
	PART,
	PCONJ,
	POSDEP,
	PRD,
	PREDEP,
	PROG,
	PUNC,
	ROOT,
	SBJ,
	TAM,
	VCL,
	VCONJ,
	VPP,
	VPRT,
	
	ANY;
	
	public static DependencyRelationType fromString(String syntaxTag){
		if (syntaxTag != null)
			for (DependencyRelationType depRel : DependencyRelationType.values()) 
				if (syntaxTag.equalsIgnoreCase(depRel.name())) 
					return depRel;
       return null;
	}
}