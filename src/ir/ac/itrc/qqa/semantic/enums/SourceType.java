package ir.ac.itrc.qqa.semantic.enums;

/**
 * Each piece of knowledge has a source.
 * @author Ehsan Darrudi
 *
 */
public enum SourceType 
{
	CORE,
	WORDNET,
	FARSNET,
	QURAN,
	TAFSIR_NEMOONE,
	ESTELAHNAME,
	FARHANG_QURAN,
	TEBYAN,
	TTS,
	UNKNOWN;
	
	public String getFarsiName()
	{
		switch (this)
		{
			case WORDNET: return "وردنت";
			case FARSNET: return "فارس نت";
			case QURAN: return "گراف مفاهیم پایه";
			case TAFSIR_NEMOONE: return "تفسیر نمونه";
			case ESTELAHNAME: return "اصطلاح نامه";
			case FARHANG_QURAN: return "فرهنگ قرآن";
			case TEBYAN: return "دانشنامه موضوعی تبیان";
			case TTS: return "کبوتر زخمی";
			case UNKNOWN: return "نامعلوم";
			default: return "نامعلوم";
		}
	}
}
