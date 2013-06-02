package opennlp.textgroundercluster.kml;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ConvertGeoCellToKML {
    private static final Logger sLogger = Logger.getLogger(ConvertGeoCellToKML.class);

    public static void main(String[] args) throws Exception {
        final String geoCellFile = args[0];
        final String kmlFile = args[1];


        mapGeoCellToKML(geoCellFile, kmlFile);

    }

    public static void mapGeoCellToKML(String inFile, String outFile) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile));
            FileWriter fileWriter = new FileWriter(outFile);

            fileWriter.write(HEADER);

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                fileWriter.write(convertLine(line));
            }
            bufferedReader.close();
            fileWriter.write(FOOTER);
            fileWriter.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String convertLine(String line) {
        // GeoCell(List((30.17,-80.06), (25.49,-80.06), (25.49,-82.75), (30.17,-82.75), (30.17,-80.06)) (Center: (26.88,-80.76)), 263 documents(dist), 263 documents(links), 9143 types, 75341.0 tokens, 0 links)
        String subString[] = line.split("Center:");
        Pattern p = Pattern.compile(".*?([-]?\\d+\\.?\\d*),([-]?\\d+\\.?\\d*)");
        Scanner sc = new Scanner(subString[0]);
        StringBuilder latLong = new StringBuilder();
        while(sc.findWithinHorizon(p, 0) != null) {
            MatchResult mr = sc.match();
            Double latitude = Double.parseDouble(mr.group(1));
            Double longitude = Double.parseDouble(mr.group(2));
            latLong.append("\t\t\t\t\t\t");
            latLong.append(longitude);
            latLong.append(",");
            latLong.append(latitude);
            latLong.append(",100\n");
        }

        return PLACEMARK_START + latLong.toString() + PLACEMARK_END;
    }
    
    private static String HEADER = "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            "\t<Style id=\"randPolyStyle\">\n" +
            "\t\t<LineStyle>\n" +
            "\t\t\t<width>1.5</width>\n" +
            "\t\t</LineStyle>\n" +
            "\t\t<PolyStyle>\n" +
            "\t\t\t<color>ffffffff</color>\n" +
            "\t\t\t<colorMode>random</colorMode>\n" +
            "\t\t</PolyStyle>\n" +
            "\t</Style>\n";
    
    private static String FOOTER = "</Document>\n" +
            "</kml>";
    
    private static String PLACEMARK_START = "\t<Placemark>\n" +
            "\t\t<name>Cell</name>\n" +
            "\t\t<styleUrl>#randPolyStyle</styleUrl>\n" +
            "\t\t<Polygon>\n" +
            "\t\t\t<extrude>1</extrude>\n" +
            "\t\t\t<altitudeMode>clampToGround</altitudeMode>\n" +
            "\t\t\t<outerBoundaryIs>\n" +
            "\t\t\t\t<LinearRing>\n" +
            "\t\t\t\t\t<coordinates>\n";
    
    private static String PLACEMARK_END = "\t\t\t\t\t</coordinates>\n" +
            "\t\t\t\t</LinearRing>\n" +
            "\t\t\t</outerBoundaryIs>\n" +
            "\t\t</Polygon>\n" +
            "\t</Placemark>";
}
