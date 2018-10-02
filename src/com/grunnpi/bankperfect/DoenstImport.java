package com.grunnpi.bankperfect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DoenstImport
{

    private List<DoenstLineBean> listCB;
    private final String path = "D:/Documents/[Mes Documents]/Gestion/_ADMIN/SCI/Decomptes ";

    public void readFile(String filename) throws ParseException
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
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

            for (String line; (line = br.readLine()) != null;)
            {
                nbLine++;
                isRealLine = false;
                // System.out.println(line);

                if (nbLine == 1)
                {
                    // raf
                }
                else if (nbLine == 2)
                {
                    // 1er ligne, num décompte + date
                    if (line.contains("SCI JEMASAREL"))
                    {
                        line = line.replace("Madame SCI JEMASAREL (126 / Relevé 0) Décompte n°", "");
                        theOwner = "SCI JEMASAREL";
                        thePath = path + "Beethoven/";
                    }
                    else if (line.contains("Mr & Mme GRUNNAGEL"))
                    {
                        line = line.replace("Mr & Mme GRUNNAGEL (55 / Relevé 0) Décompte n°", "");
                        theOwner = "Mr & Mme GRUNNAGEL";
                        thePath = path + "Bellevue/";
                    }

                    String[] lineSplit = line.split(" du ");

                    theDecompte = lineSplit[0];
                    theDateTraitement = lineSplit[1];
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
                String releveDate = theDateTraitement.substring(theDateTraitement.length() - 10);
                System.out.println(releveDate);
                String year = releveDate.substring(releveDate.length() - 4);
                String month = releveDate.substring(3, 5);
                String day = releveDate.substring(0, 2);

                File fout = new File(thePath + year + "-" + month + "-" + day + " - Décompte N°" + theDecompte + " "
                        + theOwner + ".csv");

                System.out.println(fout.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(fout);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));

                double total = 0.0;
                bw.write("Mode;Date;Tiers;Details;Montant");
                bw.newLine();
                for (DoenstLineBean cbBean : listCB)
                {

                    String stringMontant = cbBean.getMontant().replace("\"", "").replace(" ", "").replace(".", "")
                            .replace(",", ".");
                    double montant = Double.parseDouble(stringMontant);
                    total += montant;

                    bw.write(cbBean.getType() + ";" + cbBean.getDateTraitement() + ";"
                            + StringUtils.stripAccents(cbBean.getBail()) + ";"
                            + StringUtils.stripAccents(cbBean.getDescription()) + ";" + cbBean.getMontant());
                    bw.newLine();
                }
                bw.close();
                System.out.println("Total = " + total + "/Pdf = " + theSolde);
            }
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static String upperCaseFirst(String value)
    {
        // Convert String to char array.
        char[] array = value.toCharArray();
        // Modify first element in array.
        array[0] = Character.toUpperCase(array[0]);
        // Return string.
        return new String(array);
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
