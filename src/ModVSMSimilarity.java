import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.BytesRef;

public class ModVSMSimilarity extends DefaultSimilarity {

	public ModVSMSimilarity() {
		// TODO Auto-generated constructor stub
		discountOverlaps = true;
	}

	public float coord(int overlap, int maxOverlap) {
		return (float) overlap / (float) maxOverlap;
	}

	public float queryNorm(float sumOfSquaredWeights) {
		return (float) (1.0D / Math.sqrt(sumOfSquaredWeights));
	}
	
	public float lengthNorm(FieldInvertState state)
    {
        int numTerms;
        if(discountOverlaps)
            numTerms = (int) (state.getLength() - state.getNumOverlap());
        else
            numTerms = state.getLength();
        return (float)(1.0D / numTerms);
    }
	
	public float sloppyFreq(int distance) {
		return 1.0F / (float) (distance + 1);
	}

	public float scorePayload(int doc, int start, int end, BytesRef bytesref) {
		return 1.0F;
	}

	public void setDiscountOverlaps(boolean v) {
		discountOverlaps = v;
	}

	public boolean getDiscountOverlaps() {
		return discountOverlaps;
	}

	public String toString() {
		return "ModVSMSimilarity";
	}

	protected boolean discountOverlaps;
}
