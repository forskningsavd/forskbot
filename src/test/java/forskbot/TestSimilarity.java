
package forskbot;

import junit.framework.Assert;

import org.junit.Test;

import forskbot.irc.TitleSimilarity;

/**
 * 
 * @author interhack
 * 
 */
public class TestSimilarity extends AbstractTest {

	@Test
	public void testSimilarity() {

		TitleSimilarity ts = new TitleSimilarity("http://www.commodoreusa.net/CUSA_C64.aspx", "Commodore USA");
		Assert.assertTrue(ts.isSimilar());
		Assert.assertEquals(ts.getScore(), 1.0f);

		ts = new TitleSimilarity("http://www.commodoreusa.net/CUSA_C64.aspx", "commodore usa one two three four five six");
		Assert.assertFalse(ts.isSimilar());
		Assert.assertEquals(ts.getScore(), 0.25f);
	}
}
