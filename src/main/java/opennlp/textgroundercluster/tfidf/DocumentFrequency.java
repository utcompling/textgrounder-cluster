package opennlp.textgroundercluster.tfidf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentFrequency {

    Map<String, Integer> wordToDocumentFreqMap = null;
    Map<String, Integer> wordToColumnMap = null;
    List<String> orderedWordList = null;
    Integer documentsInCorpus = 0;


    public DocumentFrequency() {
    }

    public DocumentFrequency(String documentFrequencyPath) {
        ReadDocumentFrequencyFile(documentFrequencyPath);
    }

    private void ReadDocumentFrequencyFile(String documentFrequencyPath) {
        // Example of DF line
        // floor	1
        wordToDocumentFreqMap = new HashMap<String, Integer>();
        wordToColumnMap = new HashMap<String, Integer>();
        orderedWordList = new ArrayList<String>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(documentFrequencyPath));
            Integer count = 0;
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.trim().split("\t");
                if(tokens.length != 2) {
                    continue;
                }
                final String word = tokens[0];
                wordToDocumentFreqMap.put(word, Integer.parseInt(tokens[1]));
                wordToColumnMap.put(word, ++count);
                orderedWordList.add(word);
            }
            bufferedReader.close();
            documentsInCorpus = count;
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public Integer getDocumentFrequency(String word) {
        if(wordToDocumentFreqMap.containsKey(word)) {
            return wordToDocumentFreqMap.get(word);
        }
        else {
            return 0;
        }
    }
    
    public Integer getColumnNumber(String word) {
        if(wordToColumnMap.containsKey(word)) {
            return wordToColumnMap.get(word);
        }
        else {
            return 0;
        }
    }
    
    public Double computeTfIDF(String word, Integer termFrequency) {
        
        Double idf = Math.log((double) documentsInCorpus / (double) (1 + getDocumentFrequency(word)) );
        return ((double) termFrequency ) * idf;
    }

    public List<String> getAllWords() {
        return orderedWordList;
    }
}
