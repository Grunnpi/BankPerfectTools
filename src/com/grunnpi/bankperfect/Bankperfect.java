package com.grunnpi.bankperfect;

import java.io.*;
import java.text.ParseException;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.io.RandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bankperfect
{
    /** The original PDF that will be parsed. */
    public static final String PREFACE = "P://tmppierre/ya.pdf";
    /** The resulting text file. */
    public static final String RESULT = "P://tmppierre//preface.txt";

    private static final Logger LOG = LoggerFactory.getLogger(Bankperfect.class);

    public enum fileType
    {
        cb, rbc, doenst, old
    };


    public static void stuff(){

        PDFParser parser = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        PDFTextStripper pdfStripper;

        String parsedText;
        String fileName = PREFACE;
        File file = new File(fileName);
        try {
            parser = new PDFParser(new RandomAccessFile(file,"r"));
            parser.parse();

            pdfStripper = new PDFTextStripper();

            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

            parsedText = pdfStripper.getText(pdDoc);
            //System.out.println(parsedText.replaceAll("[^A-Za-z0-9. ]+", ""));
            StringTokenizer stringTokenizer = new StringTokenizer(parsedText);
            while ( stringTokenizer.hasMoreTokens() ) {
                String s = stringTokenizer.nextToken("\r\n");
                System.out.println("<" + s + ">");
            }
            //System.out.println(StringUtils.replace(parsedText,"\r","###\r"));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (cosDoc != null)
                    cosDoc.close();
                if (pdDoc != null)
                    pdDoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws ParseException, IOException
    {
        Bankperfect bankperfect = new Bankperfect();

        if ( args.length > 0 ) {
            LOG.info("a0={}",args[0]);
        }
        System.exit(0);
    }

    private void runMe(String[] args) throws ParseException
    {
        if ( args.length > 0 ) {
            LOG.info("a0={}",args[0]);
            System.exit(0);
        }

        new Bankperfect().stuff();
        System.exit(0);


        fileType myFileType = fileType.rbc;

        switch (myFileType)
        {
        case cb:
            BILImportCB bilImportCB = new BILImportCB();
            String filenameCB = "resource/CB-BIL.txt";
            bilImportCB.readFile(filenameCB);
            break;
        case old:
            RBCImportSalaireOld rbcImportSalaireOld = new RBCImportSalaireOld();
            String filenameRBCold = "resource/RBC-SalaireOld.txt";
            rbcImportSalaireOld.readFile(filenameRBCold, ".");
            break;
        case rbc:
            RBCImportSalaire rbcImportSalaire = new RBCImportSalaire();
            String filenameRBC = "resource/RBC-Salaire.txt";
            rbcImportSalaire.readFile(filenameRBC, ".");
            break;
        case doenst:
            DoenstImport doenstImport = new DoenstImport();
            String filenameDoenst = "resource/Doenst_Beethoven.txt";
            doenstImport.readFile(filenameDoenst);
            break;
        }
    }
}
