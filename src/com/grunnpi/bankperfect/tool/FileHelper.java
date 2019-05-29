package com.grunnpi.bankperfect.tool;

import com.grunnpi.bankperfect.data.Statement;
import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class FileHelper
{
    private static final Logger LOG = getLogger(FileHelper.class);
    private static final String CSV_C_FILE = "file";
    private static final String CSV_C_BANK = "bank";
    private static final String CSV_C_BRANCH = "branch";
    private static final String CSV_C_ACCOUNT = "account";
    private static final String CSV_C_DATE = "date";
    private static final String CSV_C_TIER = "tier";
    private static final String CSV_C_DESCRIPTION = "description";
    private static final String CSV_C_AMOUNT = "amount";
    private static String[] CSV_HEADERS = { CSV_C_FILE, CSV_C_BANK, CSV_C_BRANCH, CSV_C_ACCOUNT, CSV_C_DATE, CSV_C_TIER,
            CSV_C_DESCRIPTION, CSV_C_AMOUNT };

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

    private static String fileConvert(final String filename, final String fromCharset, final String toCharset)
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

    public static void saveAsCsv(final String filename, List<Statement> statements) throws IOException
    {
        // save to .CSV stuff
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filename, true), StandardCharsets.UTF_8));
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS));

        for (Statement statement : statements)
        {
            printer.printRecord("xxx", statement.getBank(), statement.getBranch(), statement.getAccount(),
                    statement.getStatementDate(), statement.getTier(), statement.getDescription(),
                    statement.getAmount());
        }
        printer.close();
        LOG.info("CSV dumped {} lines", statements.size());
    }

    public static Map<String, List<Statement>> readCsv(final String filename) throws IOException
    {
        String tempFilename = filename;
        if (filename.contains(".convertme."))
        {
            // convert file
            tempFilename = fileConvert(filename, "windows-1252", "UTF8");
        }

        Reader in = new InputStreamReader(new FileInputStream(tempFilename), Charset.forName("UTF8"));
        //Reader in = new FileReader(getCsvCacheFilename());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(CSV_HEADERS).withFirstRecordAsHeader()
                .withIgnoreEmptyLines().parse(in);

        Map<String, List<Statement>> statementPerAccount = new HashMap<>();
        for (CSVRecord record : records)
        {
            Statement statement = new Statement();

            statement.setBank(record.get(CSV_C_BANK));
            statement.setBranch(record.get(CSV_C_BRANCH));
            statement.setAccount(record.get(CSV_C_ACCOUNT));

            statement.setTier(record.get(CSV_C_TIER));
            statement.setDescription(record.get(CSV_C_DESCRIPTION));

            String statementDateString = record.get(CSV_C_DATE);

            if (statementDateString.startsWith("#"))
            {
                statement.setStatementDateVariable(statementDateString);
            }
            else
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate statementDate = LocalDate.parse(statementDateString, formatter);
                statement.setStatementDate(statementDate);
            }

            String amountString = record.get(CSV_C_AMOUNT);
            double amount = Double.parseDouble(amountString);
            statement.setAmount(amount);

            // get account ID
            if (!statementPerAccount.containsKey(statement.getBPKey()))
            {
                statementPerAccount.put(statement.getBPKey(), new ArrayList<>());
            }
            statementPerAccount.get(statement.getBPKey()).add(statement);
        }

        // here we can close stream
        in.close();

        if (filename.contains(".convertme."))
        {
            LOG.info("Delete temp file [{}] delete-{}", tempFilename, FileUtils.deleteQuietly(new File(tempFilename)));
        }

        return statementPerAccount;
    }
}
