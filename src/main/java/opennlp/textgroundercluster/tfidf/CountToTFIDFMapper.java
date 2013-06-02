package opennlp.textgroundercluster.tfidf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;

public class CountToTFIDFMapper {
    private DocumentFrequency documentFrequency;
    
    public CountToTFIDFMapper (DocumentFrequency documentFrequency) {
        this.documentFrequency = documentFrequency;
    }
    
    public void mapCountToTFIDF(String inFile, String outFile) {
        mapCountToTFIDF(inFile, outFile, 0.0);
    }
    
    public void mapCountToTFIDF(String inFile, String outFile, Double min) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile));
            FileWriter fileWriter = new FileWriter(outFile);
            
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    String convertedLine = convertLine(line, min);
                    if(convertedLine.length() == 0) continue;
                    fileWriter.write(convertedLine);
                } catch (InvalidParameterException e){
                   System.out.println("Could not parse line: " + line);
                }
                
            }
            bufferedReader.close();
            fileWriter.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private String convertLine(String line, Double min) throws InvalidParameterException {
        // Example of input line
        // USER_79321756	47.528139,-122.197916	new:2 boys:1 jk:2 crazy:1 rollin:1 peppers:1 salt:1
        StringBuilder result = new StringBuilder();
        String[] lineTokens = line.trim().split("\t");
        result.append(lineTokens[0]);
        result.append("\t");
        result.append(lineTokens[1]);
        result.append("\t");
        String[] wordCountTokens;
        if(lineTokens.length == 3)  {
            wordCountTokens = lineTokens[2].split("\\s");
        }      else if (lineTokens.length == 10) {
            wordCountTokens = lineTokens[9].split("\\s");
        } else {
            throw new InvalidParameterException(line);
        }
        StringBuilder wordCountBuilder = new StringBuilder();
        for (String wordCountToken : wordCountTokens) {
            String[] wordCount = wordCountToken.trim().split(":");
            final String term = wordCount[0];
            final int termFrequency = Integer.parseInt(wordCount[1]);
            Double tfidf = documentFrequency.computeTfIDF(term, termFrequency);
            if (tfidf < min) continue;
            wordCountBuilder.append(documentFrequency.getColumnNumber(term));
            wordCountBuilder.append(":");
            wordCountBuilder.append(tfidf);
            wordCountBuilder.append(" ");
        }
        if(wordCountBuilder.length() == 0) return "";
        result.append(wordCountBuilder.toString());
        result.append("\r\n");

        return result.toString();
    }
}
