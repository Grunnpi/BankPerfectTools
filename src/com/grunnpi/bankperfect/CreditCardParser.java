package com.grunnpi.bankperfect;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreditCardParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(CreditCardParser.class);
    private static final String TOTAL_DES_MOUVEMENTS = "Total des mouvements - ";

    public List<Statement> prepare(List<String> lines,Map<String,String> mapping)
    {

        double amountTotal = 0;

        List<Statement> listStatement = new ArrayList<Statement>();

        LOG.info("Prepare[{}] lines",lines.size());
        {
            int nbLine = 0;
            int nbLineOk = 0;
            int nbLineNok = 0;
            String addComment = "";
            Statement newStatement = null;
            for (String line : lines)
            {
                nbLine++;

                if ( line.length() <= 2 ) {
                    //
                }
                else if ( line.startsWith(TOTAL_DES_MOUVEMENTS)) {
                    String total = line.replace(TOTAL_DES_MOUVEMENTS,"");
                    total = total.replace(".","");
                    total = total.replace(",",".");
                    amountTotal = Double.parseDouble(total) * -1;
                }
                else if (nbLine == 1)
                {
                    // 1er ligne, c'est le libellé commun
                    addComment = line;
                }
                else if (line.charAt(2) == '/')
                {
                    // prepare a new statement
                    newStatement = new Statement();
                    newStatement.setRawLine(line);
                    newStatement.setValid(true);

                    final String statementRawDate = line.substring(0,10);
                    final String statementYear = statementRawDate.substring(statementRawDate.length() - 4);
                    final String statementMonth = statementRawDate.substring(3,5);
                    final String statementDay = statementRawDate.substring(0,2);
                    LOG.info("[{}] // [{}-{}-{}]",statementRawDate,statementYear,statementMonth,statementDay);
                    LocalDate localDate = LocalDate.of(Integer.valueOf(statementYear), Integer.valueOf(statementMonth), Integer.valueOf(statementDay));

                    newStatement.setStatementDate(localDate);

                    String couldBeAmount = StringUtils.substringAfterLast(line.trim(),"-");
                    couldBeAmount = couldBeAmount.replace(",",".");
                    double amount = Double.parseDouble(couldBeAmount) * -1;
                    newStatement.setAmount(amount);

                    String description = StringUtils.substringBeforeLast(line.trim(),"-").trim();
                    description = description.substring(22);

                    newStatement.setDescription(description);
                }
                else
                {
                    // retour à la ligne pour détail en plus sur la description
                    //LOG.warn("line CR[{}]",line);
                }

                if (newStatement != null)
                {
                    listStatement.add(newStatement);
                    newStatement = null;
                }
            }
        }

        double amountTotalComputed = 0;
        for ( Statement statement : listStatement) {
            amountTotalComputed += statement.getAmount();
        }

        // checksum final
        DecimalFormat df = new DecimalFormat("#.00");
        if ( amountTotal != 0.0 ) {
            if ( areEqualByThreeDecimalPlaces(amountTotal,amountTotalComputed) ) {
                LOG.info("Total [{}]==[{}] is OK",df.format(amountTotal),df.format(amountTotalComputed));
            }
            else {
                LOG.error("Total [{}]<>[{}] is NOT OK",df.format(amountTotal),df.format(amountTotalComputed));
            }
        }
        else {
            LOG.warn("AmountTotal [{}]",df.format(amountTotal));
        }

        return listStatement;
    }

    public static boolean areEqualByThreeDecimalPlaces(double a, double b)
    {
        a = a * 100;
        b = b * 100;
        int a1 = (int) a;
        int b1 = (int) b;
        if (a1 == b1)
        {
            return true;
        }
        return false;
    }
}