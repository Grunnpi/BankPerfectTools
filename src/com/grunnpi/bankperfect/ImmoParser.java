package com.grunnpi.bankperfect;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ImmoParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(ImmoParser.class);

    private List<DoenstLineBean> listCB;
    private final String path = "D:/Documents/[Mes Documents]/Gestion/_ADMIN/SCI/Decomptes ";

    public List<Statement> prepare(List<String> lines,Map<String,String> mapping, final String accountSignature)
    {
        List<Statement> listStatement = new ArrayList<Statement>();
        {
            int nbLine = 0;
            int nbLineOk = 0;
            int nbLineNok = 0;
            String addComment = "";
            listCB = new ArrayList<DoenstLineBean>();
            DoenstLineBean lineBean = null;

            String theBien = "";
            String theBail = "";
            String theTypeLot = "";
            String theOperation = "";
            boolean theSignePositive = true;
            String theDescription = "";
            boolean isRealLine = false;
            String theDateTransaction = "";
            String theDateTransactionLast = "";
            String theDateTransactionLastFormatted = "";
            String theMontant = "";
            String theType = "";

            String theDecompte = "";
            String theDateTraitement = "";

            String theSolde = "";

            boolean isEcritureLot = false;
            boolean isStuffBail = false;

            String theOwner = "";
            String thePath = "";

            boolean immoOwnerFound = false;
            for (String line : lines )
            {
                nbLine++;
                isRealLine = false;
                LOG.info("#{} [{}]",nbLine,line);

                if ( !immoOwnerFound )
                {
                    // 1er ligne, num décompte + date
                    if (line.contains("SCI JEMASAREL"))
                    {
                        line = line.replace("Madame SCI JEMASAREL (126 / Relevé 0) Décompte n°", "");
                        theOwner = "SCI JEMASAREL";
                        thePath = path + "Beethoven/";
                        immoOwnerFound = true;
                    }
                    else if (line.contains("Mr & Mme GRUNNAGEL"))
                    {
                        line = line.replace("Mr & Mme GRUNNAGEL (55 / Relevé 0) Décompte n°", "");
                        theOwner = "Mr & Mme GRUNNAGEL";
                        thePath = path + "Bellevue/";
                        immoOwnerFound = true;
                    }

                    String[] lineSplit = line.split(" du ");
                    if ( lineSplit.length > 1 ) {
                        theDecompte = lineSplit[0];
                        theDateTraitement = lineSplit[1];
                    }
                    else {
                        // skip this line
                        LOG.info(">> skip this line");
                    }
                }
                else if (line.startsWith("Sous-totaux "))
                {
                    isEcritureLot = false;
                    isStuffBail = false;
                }
                else if (line.startsWith("Bail "))
                {
                    theBail = line.replace("Bail ", "");
                    isStuffBail = true;
                }

                else if (line.startsWith("Immeuble 36 rue Beethoven"))
                {
                    theBien = "Beethoven";
                    isStuffBail = false;
                }
                else if (line.startsWith("Maison 6, rue Bellevue"))
                {
                    theBien = "Bellevue";
                    isStuffBail = false;
                }
                else if (line.startsWith("Ecritures de l'immeuble"))
                {
                    if (theBien.equals("Bellevue"))
                    {
                        theBail = "Gestion Maison";
                        theTypeLot = "Maison";
                    }
                    else
                    {
                        theBail = "Gestion Immeuble";
                        theTypeLot = "Immeuble";
                    }

                    theSignePositive = false;
                    isStuffBail = false;
                }
                else if (line.startsWith("Lot 36 rue Beethoven, "))
                {
                    theTypeLot = line.replace("Lot 36 rue Beethoven, ", "");

                    if (theTypeLot.contains("Appartement Type 3"))
                    {
                        theTypeLot = "Appartement";
                    }
                    theSignePositive = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("Lot 6, rue Bellevue, Maison Type 7 et +"))
                {
                    theTypeLot = "Maison";
                    theSignePositive = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("Ecritures du lot"))
                {
                    theSignePositive = false;
                    isEcritureLot = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("ORPI DOENST IMMO"))
                {
                    isEcritureLot = false;
                    isStuffBail = false;
                }
                else if (line.startsWith("Solde en votre faveur au "))
                {
                    theSolde = line.replace("Solde en votre faveur au ", "");
                    theSolde = StringUtils.substringAfter(theSolde, " ");
                    isStuffBail = false;
                }
                else if (line.startsWith("Loyer ") || line.startsWith("Provision charges courantes ")
                        || line.startsWith("Garantie loyers impayés (GLI)") || line.startsWith("Honoraires de gestion ")
                        || line.startsWith("Assurance loyers impayés") || isStuffBail)
                {
                    if (line.startsWith("Provision charges courantes "))
                    {
                        theOperation = "Charge";
                        line = line.replace("Provision charges courantes ", "");
                    }
                    else if (line.startsWith("Loyer "))
                    {
                        theOperation = "Loyer";
                        line = line.replace("Loyer ", "");
                    }
                    else if (line.startsWith("Garantie loyers impayés (GLI)"))
                    {
                        theOperation = "Assurance";

                        line = line.replace("Garantie loyers impayés (GLI) ", "");
                        line = StringUtils.substringAfter(line, ") ");
                        line = theDateTransactionLast + " 5,84 " + line;
                    }
                    else if (line.startsWith("Assurance loyers impayés"))
                    {
                        theOperation = "Assurance";
                        line = line.replace("Assurance loyers impayés ", theDateTransactionLast + " 5,84 ");
                    }
                    else if (line.startsWith("Honoraires de gestion 5,84% HT "))
                    {
                        theOperation = "Frais";
                        line = theDateTransactionLast + " 5,84 " + StringUtils.substringAfterLast(line, ") ");
                    }
                    else if (line.startsWith("Honoraires de gestion 7,00% HT "))
                    {
                        theOperation = "Frais";
                        line = theDateTransactionLast + " 5,84 " + StringUtils.substringAfterLast(line, ") ");
                    }
                    else
                    {
                        theOperation = "Loyer";
                        line = line.replace("Loyer ", "");
                    }

                    String[] split = null;
                    if (line.contains(" 5,84 "))
                    {
                        split = line.split(" 5,84 ");
                    }
                    else if (line.contains(" 7,00 "))
                    {
                        split = line.split(" 7,00 ");
                    }
                    else
                    {
                        System.err.println("line [" + line + "]");
                    }

                    theDateTransaction = split[0];

                    theDateTransaction = theDateTransaction.replace(" (solde)", "");
                    theDateTransaction = theDateTransaction.replace(" (partiel)", "");
                    theDateTransaction = theDateTransaction.replace(" (reliquat)", "");
                    // theDateTransaction = theDateTransaction.toLowerCase();

                    theDateTransactionLast = theDateTransaction;

                    String[] dateSplit = theDateTransaction.split(" ");

                    try
                    {
                        String mois = dateSplit[0];
                        if (mois.equals("AOUT"))
                        {
                            mois = "Août";
                        }
                        else if (mois.equals("DECEMBRE"))
                        {
                            mois = "Décembre";
                        }
                        Date date = new SimpleDateFormat("MMMM").parse(mois);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        String annee = dateSplit[1];
                        if (annee.length() == 2)
                        {
                            annee = "20" + annee;
                        }

                        theDateTransaction = String.format("01/%02d/%s", cal.get(Calendar.MONTH) + 1, annee);

                        theDateTransactionLastFormatted = theDateTransaction;
                    }
                    catch (Exception e)
                    {
                        System.err.println("Date = " + theDateTransaction);
                    }

                    // TODO: faut virer le nom du bail dans la description
                    theDescription = split[0];

                    theDescription = theDescription.replace(" MME GASPARINI CECIL", "");
                    theDescription = theDescription.replace(" MLLE MICHELE NGATOU", "");
                    theDescription = theDescription.replace(" MME JEANNINE FRIGOL", "");

                    theMontant = theSignePositive ? split[1] : "-" + split[1];
                    theType = theSignePositive ? "Versement" : "Versement";

                    isRealLine = true;
                }

                else if (isEcritureLot)
                {
                    // Ligne de travaux
                    theDescription = StringUtils.substringBeforeLast(line, " ");

                    theMontant = StringUtils.substringAfterLast(line, " ");
                    theMontant = theSignePositive ? theMontant : "-" + theMontant;

                    theType = "Versement";
                    isRealLine = true;

                    if (theDescription.contains("CONTRAT ENTRETIEN"))
                    {
                        theOperation = "Entretien";
                    }
                    else
                    {
                        theOperation = "Travaux";
                    }

                    theDateTransaction = theDateTransactionLastFormatted;
                }

                if (isRealLine)
                {
                    lineBean = new DoenstLineBean();

                    lineBean.setBail(theBail);
                    lineBean.setLot(theTypeLot);
                    lineBean.setOperation(theOperation);
                    lineBean.setDateTransaction(theDateTransaction);
                    lineBean.setDateTraitement(theDateTraitement);
                    lineBean.setMontant(theMontant);
                    lineBean.setType(theType);
                    lineBean.setDescription(theBien + "/Decompte_" + theDecompte + "/"
                            + upperCaseFirst(theDescription.toLowerCase()) + "/" + theTypeLot + "/" + theOperation);

                    System.out.println(lineBean);

                    listCB.add(lineBean);
                }
            }

            System.out.println("Nb Ligne = " + listCB.size());

            {
                if ( theDateTraitement.length() > 10) {
                    String releveDate = theDateTraitement.substring(theDateTraitement.length() - 10);
                    System.out.println(releveDate);
                    double total = 0.0;
                    for (DoenstLineBean cbBean : listCB)
                    {

                        String stringMontant = cbBean.getMontant().replace("\"", "").replace(" ", "").replace(".", "")
                                .replace(",", ".");
                        double montant = Double.parseDouble(stringMontant);
                        total += montant;
                    }
                    System.out.println("Total = " + total + "/Pdf = " + theSolde);
                }
                else {
                    LOG.error("No date ?! [{}]",theDateTraitement);
                }

            }
        }
        return listStatement;
    }

    public static String upperCaseFirst(String value)
    {
        if ( !StringUtils.isEmpty(value) ) {
            // Convert String to char array.
            char[] array = value.toCharArray();
            // Modify first element in array.
            array[0] = Character.toUpperCase(array[0]);
            // Return string.
            return new String(array);
        }
        else  return "";
    }

    /**
     * @return the listCB
     */
    public List<DoenstLineBean> getListCB()
    {
        return listCB;
    }

    /**
     * @param listCB the listCB to set
     */
    public void setListCB(List<DoenstLineBean> listCB)
    {
        this.listCB = listCB;
    }
}
