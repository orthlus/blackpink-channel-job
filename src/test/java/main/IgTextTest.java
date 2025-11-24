package main;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class IgTextTest {
	@Test
	public void testEnrichLinks() {
		String srcText = srcText();
		String s = srcText.replaceAll("@([\\w_.]+)", "<a href=\"https://www.instagram.com/$1\">@$1</a>");
		System.out.println(s);
		assertThat(s, containsString("https://www.instagram.com/renellmedrano"));
	}

	private String srcText() {
		return """
				JENNIE (https://www.instagram.com/p/DPTFouGDize)
				
				Very Very happy to share this
				
				CR Fashion Book Issue 27 Confidential
				
				@crfashionbook
				
				Photography @renellmedrano
				Stylist @benperreira
				Hair @leeseonyeong__
				Makeup @joynara
				Nails @nailsbyzola
				Editor-in-Chief @carineroitfeld
				CEO @vladimirrestoinroitfeld
				Creative Director @e.mman
				Art Director @guillaumelauruol
				BTS pics @choeey_
				
				#JENNIE""";
	}
}
