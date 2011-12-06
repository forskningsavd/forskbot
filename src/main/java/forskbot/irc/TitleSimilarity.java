
package forskbot.irc;

/**
 * 
 * @author interhack
 * 
 */
public class TitleSimilarity {

	public static final int WORDLEN_THRESH = 2;
	//
	private int keywordsTotal = 0;
	private int keywordsContained = 0;
	//
	private float score = 0f;

	public TitleSimilarity(String uri, String title) {

		matchFilteredContained(uri.toLowerCase(), title.toLowerCase());
	}

	private void matchFilteredContained(String uri, String title) {

		for (String word : title.split("\\s+")) {
			if (word.length() >= WORDLEN_THRESH && word.matches("\\w+")) {
				keywordsTotal++;
				if (uri.contains(word)) {
					keywordsContained++;
				}
			}
		}
	}

	public boolean isSimilar() {

		if (keywordsTotal == 0 || keywordsContained == 0) {
			return false;
		}

		score = (float) keywordsContained / keywordsTotal;
		if (score >= 0.3f) {
			return true;
		} else {
			return false;
		}
	}

	public float getScore() {

		return score;
	}
}
