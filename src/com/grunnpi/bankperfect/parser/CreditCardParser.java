package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreditCardParser extends AbstractParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(CreditCardParser.class);
    private static final String TOTAL_DES_MOUVEMENTS = "Total des mouvements - ";

    public List<Statement> prepare(BankFile bankFile, List<String> lines, Map<String, String> mapping,
            final String accountSignature)
    {
        double amountTotal = 0;

        String cardType = "";
        String releveDate = "";

        List<Statement> listStatement = new ArrayList<Statement>();
        LOG.info("Prepare[{}] lines", lines.size());
        {
            Statement newStatement = null;
            for (String line : lines)
            {
                String fullLine = line;
                if (line.length() <= 2)
                {
                    //
                }
                else if (line.startsWith(TOTAL_DES_MOUVEMENTS))
                {
                    String total = line.replace(TOTAL_DES_MOUVEMENTS, "");
                    total = total.replace(".", "");
                    total = total.replace(",", ".");
                    amountTotal = Double.parseDouble(total) * -1;
                }
                else if (line.startsWith("Relevé de carte(s) au"))
                {
                    releveDate = line.replace("Relevé de carte(s) au ", "");
                    LOG.info("Relevé de carte(s) au [{}]", releveDate);
                }
                else if (line.startsWith("Mouvement"))
                {
                    if (line.contains("VISA"))
                    {
                        cardType = "VISA";
                    }
                    else
                    {
                        cardType = "MASTERCARD";
                    }
                    LOG.info("CreditCard[{}]", cardType);
                }
                else if (line.charAt(2) == '/')
                {
                    // prepare a new statement
                    newStatement = new Statement();
                    newStatement.setRawLine(line);
                    newStatement.setValid(true);

                    newStatement.setAccountID(getAccount(accountSignature, cardType));

                    final String statementRawDate = line.substring(0, 10);
                    final String statementYear = statementRawDate.substring(statementRawDate.length() - 4);
                    final String statementMonth = statementRawDate.substring(3, 5);
                    final String statementDay = statementRawDate.substring(0, 2);
                    //                    LOG.info("[{}] // [{}-{}-{}]",statementRawDate,statementYear,statementMonth,statementDay);
                    LocalDate localDate = LocalDate.of(Integer.valueOf(statementYear), Integer.valueOf(statementMonth),
                            Integer.valueOf(statementDay));

                    newStatement.setStatementDate(localDate);

                    String couldBeAmount = StringUtils.substringAfterLast(line.trim(), "-");
                    double amount = 0;
                    String description = "";

                    couldBeAmount = couldBeAmount.replace(",", ".");
                    if (StringUtils.isEmpty(couldBeAmount))
                    {

                        // could be positive amount - refund ?
                        couldBeAmount = StringUtils.substringAfterLast(line.trim(), "+");
                        couldBeAmount = couldBeAmount.replace(",", ".");

                        if (StringUtils.isEmpty(couldBeAmount))
                        {
                            LOG.error("No amount [{}] on line [{}]", couldBeAmount, fullLine);
                        }
                        else
                        {
                            amount = Double.parseDouble(couldBeAmount);
                            description = StringUtils.substringBeforeLast(line.trim(), "+").trim();
                        }
                    }
                    else
                    {
                        // negative amount
                        amount = Double.parseDouble(couldBeAmount) * -1;
                        description = StringUtils.substringBeforeLast(line.trim(), "-").trim();
                    }

                    newStatement.setAmount(amount);
                    description = description.substring(22);

                    description = description + "-RELEVE " + cardType + " AU " + releveDate;

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
        for (Statement statement : listStatement)
        {
            amountTotalComputed += statement.getAmount();
        }

        // checksum final
        DecimalFormat df = new DecimalFormat("#.00");
        if (amountTotal != 0.0)
        {
            if (areEqualByThreeDecimalPlaces(amountTotal, amountTotalComputed))
            {
                LOG.info("Total [{}]==[{}] is OK", df.format(amountTotal), df.format(amountTotalComputed));
            }
            else
            {
                LOG.error("Total [{}]<>[{}] is NOT OK", df.format(amountTotal), df.format(amountTotalComputed));
            }
        }
        else
        {
            LOG.warn("AmountTotal [{}]", df.format(amountTotal));
        }


        final String statementYear = releveDate.substring(releveDate.length() - 4);
        final String statementMonth = releveDate.substring(3, 5);
        final String statementDay = releveDate.substring(0, 2);

        bankFile.setToRename(true);
        bankFile.setToMoveToArchive(true);
        bankFile.setTargetName(getArchiveDir() + "/" + statementYear + "-" + statementMonth + "-" + statementDay + "-" + cardType + ".pdf");

        return listStatement;
    }
}