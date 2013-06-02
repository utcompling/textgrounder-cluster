package opennlp.textgroundercluster.tfidf;

/**
 * Converts the count file to an ARFF file using TF-IDF
 */
public class ConvertCountToTFIDF {
    public static void main(String[] args) throws Exception {
        //Configuration conf = new Configuration();
        final String inFile = args[0];
        final String tfidfFile = args[1];
        final String documentFrequencyPath = args[2];
        Double min = 0.0;
        if(args.length == 4) {
            min = Double.parseDouble(args[3]);
        }

        DocumentFrequency documentFrequency = new DocumentFrequency(documentFrequencyPath);

        CountToTFIDFMapper countToTFIDFMapper = new CountToTFIDFMapper(documentFrequency);
        countToTFIDFMapper.mapCountToTFIDF(inFile, tfidfFile, min);
    }
}
