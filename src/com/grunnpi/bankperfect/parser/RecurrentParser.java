package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import com.grunnpi.bankperfect.tool.FileHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecurrentParser extends AbstractParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(RecurrentParser.class);

    @Override
    public List<Statement> prepare(BankFile bankFile, List<String> lines, Map<String, String> mapping,
            String accountSignature)
    {
        return null;
    }

    @Override
    public List<Statement> processFiles() throws IOException
    {
        List<Statement> statements = new ArrayList<>();

        List<BankFile> listBankFiles = getListBankFiles();
        Map<String, List<Statement>> statementPerAccount = new HashMap<>();
        for (BankFile bankFile : listBankFiles)
        {

            LOG.info("process[{}]", bankFile.getFile().getName());

            Map<String, List<Statement>> statementForThisAccount = FileHelper
                    .readCsv(bankFile.getFile().getAbsolutePath());
            if (statementForThisAccount != null)
            {
                LOG.info("process.r[{}].nbAccount[{}]", bankFile.getFile().getName(), statementForThisAccount.size());
                for (Map.Entry<String, List<Statement>> e : statementForThisAccount.entrySet())
                {
                    List<Statement> statements1;
                    if (!statementPerAccount.containsKey(e.getKey()))
                    {
                        statements1 = new ArrayList<>();
                        statementPerAccount.put(e.getKey(), statements1);
                    }
                    else
                    {
                        statements1 = statementPerAccount.get(e.getKey());
                    }
                    statements1.addAll(e.getValue());
                    statementPerAccount.put(e.getKey(), statements1);
                }
            }
            else
            {
                LOG.info("process.r[{}] no stuff here", bankFile.getFile().getName());
            }

            bankFile.setToRename(false);

            bankFile.setToMoveToArchive(true);
            bankFile.setTargetName(getArchiveDir() + "/" + bankFile.getFile().getName());
        }

        if (statementPerAccount.size() > 0)
        {
            // collect single date
            Map<String, LocalDate> mapVariableDate = new HashMap<>();
            for (Map.Entry<String, List<Statement>> mapStatements : statementPerAccount.entrySet())
            {
                for (Statement statement : mapStatements.getValue())
                {
                    LOG.info("Statement[{}]", statement.getDescription());
                    if (!StringUtils.isEmpty(statement.getStatementDateVariable()))
                    {
                        if (!mapVariableDate.containsKey(statement.getStatementDateVariable()))
                        {
                            mapVariableDate.put(statement.getStatementDateVariable(), null);
                        }
                    }
                    statements.add(statement);
                }
            }

            // input each
            if (mapVariableDate.size() > 0)
            {

                for (Map.Entry<String, LocalDate> entry : mapVariableDate.entrySet())
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Input [" + entry.getKey() + "] (YYYY-MM-DD)");
                    String statementDateString = br.readLine();
                    try
                    {
                        LocalDate statementDate = LocalDate.parse(statementDateString);
                        mapVariableDate.put(entry.getKey(), statementDate);
                    }
                    catch (Exception e)
                    {
                        LOG.error("Date cannot be cast [{}]", statementDateString, e);
                    }
                }

                // assign it
                for (Statement statement : statements)
                {
                    if (!StringUtils.isEmpty(statement.getStatementDateVariable()))
                    {
                        statement.setStatementDate(mapVariableDate.get(statement.getStatementDateVariable()));
                    }
                }
            }
        }

        return statements;
    }
}
