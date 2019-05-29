package com.grunnpi.bankperfect.tool;

import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class FileHelper
{
    private static final Logger LOG = getLogger(FileHelper.class);

    public static Map<String, String> readFileMap(File file)
    {
        Map<String, String> myMap = null;
        try
        {
            myMap = new HashMap<>();
            if (file.exists() && FileUtils.sizeOf(file) > 0)
            {
                List<String> excludeLines = FileUtils.readLines(file, "UTF-8");
                for (String line : excludeLines)
                {
                    if (line.contains("="))
                    {
                        final String key = StringUtils.substringBefore(line, "=");
                        final String value = StringUtils.substringAfter(line, "=");
                        myMap.put(key, value);
                        //                        LOG.info("[{}]=[{}]",key,value);
                    }
                }
                LOG.debug("Map[{}].size={}", file.getAbsolutePath(), myMap.size());
            }
            else
            {
                LOG.debug("No map found [{}]", file.getName());
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error", file.getName(), e);
        }

        return myMap;
    }

    public static List<String> readFullPdf(File pdfFile, File exclude, final boolean layoutStripper)
    {
        List<String> lines = new ArrayList<>();
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        String parsedText;
        try
        {
            PDFParser parser = new PDFParser(new RandomAccessFile(pdfFile, "r"));
            parser.parse();
            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

            PDFTextStripper pdfStripper = new PDFTextStripper();

            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();

            if (layoutStripper)
            {
                parsedText = pdfTextStripper.getText(pdDoc);
            }
            else
            {
                parsedText = pdfStripper.getText(pdDoc);
            }

            // lines to ignore
            String[] strings = readFileArray(exclude);

            StringTokenizer stringTokenizer = new StringTokenizer(parsedText);
            while (stringTokenizer.hasMoreTokens())
            {
                String s = stringTokenizer.nextToken("\r\n");
                s = StringUtils.trimToEmpty(s);
                if (!StringUtils.startsWithAny(s, strings) && !StringUtils.isEmpty(s))
                {
                    if (layoutStripper)
                    {
                        s = s.trim().replaceAll(" +", " ");
                    }
                    //                    LOG.info("{}",s);
                    lines.add(s);
                }
            }
            cosDoc.close();
            //System.out.println(StringUtils.replace(parsedText,"\r","###\r"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                if (cosDoc != null)
                    cosDoc.close();
                if (pdDoc != null)
                    pdDoc.close();
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
        }
        return lines;
    }

    private static String[] readFileArray(File file)
    {
        List<String> excludeLines;
        String[] strings = {};
        try
        {
            if (file.exists() && FileUtils.sizeOf(file) > 0)
            {
                excludeLines = FileUtils.readLines(file, "UTF-8");
            }
            else
            {
                LOG.debug("No exclude found [{}]", file.getName());
                excludeLines = new ArrayList<>();
            }

            if (excludeLines.size() > 0)
            {
                strings = excludeLines.toArray(new String[0]);
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error", file.getName(), e);
        }

        return strings;
    }

    private static String renameConverted(final String filename)
    {
        return filename.replace(".convertme.", ".converted.") + ".tmp";
    }

    public static String fileConvert(final String filename, final String fromCharset, final String toCharset)
    {
        String tempFilename = renameConverted(filename);
        try
        {
            FileInputStream fis = new FileInputStream(filename);
            byte[] contents = new byte[fis.available()];
            fis.read(contents, 0, contents.length);
            String asString = new String(contents, fromCharset);
            byte[] newBytes = asString.getBytes(toCharset);

            FileOutputStream fos = new FileOutputStream(tempFilename);
            fos.write(newBytes);
            fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        LOG.info("Converted[{}] from [{}] to [{}]", filename, fromCharset, toCharset);
        return tempFilename;
    }
}
