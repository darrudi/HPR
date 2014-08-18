package ir.ac.itrc.qqa.semantic.kb;

import ir.ac.itrc.qqa.semantic.util.Common;

 /** 
 * Each statement in HPR has several certainty parameters attached to it. These are:
 * <ul>
 * 		<li> γ: Certainty </li> 
 * 		<li> φ: Frequency </li> 
 * 		<li> τ: Typicality </li>
 * 		<li> σ: Similarity </li>
 * 		<li> δ: Dominance </li>
 * 		<li> α: Conditional Likelihood </li>
 * 		<li> β: Reverse Conditional Likelihood </li>
 * 		<li> µa: argument Multiplicity </li>
 * 		<li> µr: referent Multiplicity </li>
 * </ul>
 * and several others which aren't implemented in this project.
 * Please refer to the original paper of HPR for definition and more details.
 * This class encapsulates all these parameters in one class 
 * plus methods useful to serialize them. 
 * 
 * @author Ehsan Darrudi
 */
public class CertaintyParameters
{
	public float certainty;
	public float frequency;
	public float typicality;
	public float similarity;
	public float dominance;
	public float conditionalLikelihood;
	public float reverseConditionalLikelihood;
	public float argumentMultiplicity;
	public float referentMultiplicity;
	
	public static Float defaultCertainty = 0.99F;
	
	/**
	 * Loads certainty parameters using an array 
	 * @param params an array of float numbers which will be assigned to the certainty parameters
	 */
	public CertaintyParameters(float[] params)
	{
		certainty					= params[0];
		frequency					= params[1];
		typicality					= params[2];
		similarity					= params[3];
		dominance					= params[4];
		conditionalLikelihood		= params[5];
		reverseConditionalLikelihood = params[6];
		argumentMultiplicity		= params[7];
		referentMultiplicity		= params[8];
	}
	
	/**
	 * loads certainty parameters using the values of another instance of this class
	 * @param cp another instance of this class.
	 */
	public CertaintyParameters(CertaintyParameters cp)
	{
		certainty					= cp.certainty;
		frequency					= cp.frequency;
		typicality					= cp.typicality;
		similarity					= cp.similarity;
		dominance					= cp.dominance;
		conditionalLikelihood		= cp.conditionalLikelihood;
		reverseConditionalLikelihood= cp.reverseConditionalLikelihood;
		argumentMultiplicity		= cp.argumentMultiplicity;
		referentMultiplicity		= cp.referentMultiplicity;
	}
	
	/**
	 * Loads default certainty parameters
	 */
	private void loadDefaults()
	{
		certainty					= defaultCertainty;
		frequency					= defaultCertainty;
		typicality					= defaultCertainty;
		similarity					= defaultCertainty;
		dominance					= defaultCertainty;
		conditionalLikelihood		= defaultCertainty;
		reverseConditionalLikelihood= defaultCertainty;
		argumentMultiplicity		= defaultCertainty;
		referentMultiplicity		= defaultCertainty;
	}
	
	/**
	 * Loads certainty parameters with defaults.
	 * <p>
	 * <b> Warning: </b> the default value for all parameters is 0.99F which means <em>unknown</em>.
	 */
	public CertaintyParameters()
	{
		loadDefaults();
	}

    /**
     * Loads certainty parameters using a string
     * 
     * @param text a string representing the parameters 
	 * @see #toString()
     */
	public CertaintyParameters(String text)
	{
		if (!text.startsWith("["))
		{	
			loadDefaults();
			
			return;
		}

		certainty = ExtractParameter(text, "γ = ");
		frequency = ExtractParameter(text, "φ = ");
		typicality = ExtractParameter(text, "τ = ");
		similarity = ExtractParameter(text, "σ = ");
		dominance = ExtractParameter(text, "δ = ");
		conditionalLikelihood = ExtractParameter(text, "α = ");
		reverseConditionalLikelihood = ExtractParameter(text, "β = ");
		argumentMultiplicity = ExtractParameter(text, "µa = ");
		referentMultiplicity = ExtractParameter(text, "µr = ");
	}

    /**
     * Loads a certainty parameter using a snippet of text 
     * @param text
     * @param snippet
     * @return the value of the extracted parameter
     */
    private float ExtractParameter(String text, String snippet)
	{
		int Pos = text.indexOf(snippet);
		
		if (Pos == -1)
		{
			return defaultCertainty;
		}

		Pos += snippet.length();
		
		int Length = 0;
		
		while (text.charAt(Pos + Length) != ',' && text.charAt(Pos + Length) != ']')
		{
			Length++;
		}

		return Float.parseFloat(text.substring(Pos, Pos + Length));
	}

    @Override
    public String toString()
	{
		String temp = "";

		temp +=	(certainty == defaultCertainty)						? "" : "γ = " + String.format("%.5f", certainty);
		temp += (frequency == defaultCertainty) 					? "" : " , φ = " + String.format("%.5f", frequency);
		temp += (typicality == defaultCertainty) 					? "" : " , τ = " + String.format("%.5f", typicality);
		temp += (similarity == defaultCertainty) 					? "" : " , σ = " + String.format("%.5f", similarity);
		temp += (dominance == defaultCertainty) 					? "" : " , δ = " + String.format("%.5f", dominance);
		temp += (conditionalLikelihood == defaultCertainty) 		? "" : " , α = " + String.format("%.5f", conditionalLikelihood);
		temp += (reverseConditionalLikelihood == defaultCertainty) 	? "" : " , β = " + String.format("%.5f", reverseConditionalLikelihood);
		temp += (argumentMultiplicity == defaultCertainty) 			? "" : " , µa = " + String.format("%.5f", argumentMultiplicity);
		temp += (referentMultiplicity == defaultCertainty) 			? "" : " , µr = " + String.format("%.5f", referentMultiplicity);

		if (temp.equals(""))
			return "";
		
		temp = "[" + temp + "]";
		
		return temp.replace("[ , ", "[");
	}
    
    /**
     * Produces a qualitative representation of certainty parameters 
     * @return a qualitative representation of certainty parameters  
     */
	public String ToStringQualitative()
	{
		String Temp;

		Temp =	"[γ = " + Common.qualifyNumber(certainty);
		Temp += (frequency == defaultCertainty) ? "" : " , φ = " + Common.qualifyNumber(frequency);
		Temp += (typicality == defaultCertainty) ? "" : " , τ = " + Common.qualifyNumber(typicality);
		Temp += (similarity == defaultCertainty) ? "" : " , σ = " + Common.qualifyNumber(similarity);
		Temp += (dominance == defaultCertainty) ? "" : " , δ = " + Common.qualifyNumber(dominance);
		Temp += (conditionalLikelihood == defaultCertainty) ? "" : " , α = " + Common.qualifyNumber(conditionalLikelihood);
		Temp += (reverseConditionalLikelihood == defaultCertainty) ? "" : " , β = " + Common.qualifyNumber(reverseConditionalLikelihood);
		Temp += (argumentMultiplicity == defaultCertainty) ? "" : " , µa = " + Common.qualifyNumber(argumentMultiplicity);
		Temp += (referentMultiplicity == defaultCertainty) ? "" : " , µr = " + Common.qualifyNumber(referentMultiplicity);

		Temp += "]";

		return Temp;
	}
}
