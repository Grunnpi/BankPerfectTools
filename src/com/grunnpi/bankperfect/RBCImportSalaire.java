package com.grunnpi.bankperfect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class RBCImportSalaire
{

    private static final String REPORT_DEJA_EFFECTUES = "Reports déjà effectués";
    private List<RBCLineBean> listRBC;
    private final String path = "D:/Documents/[Mes Documents]/Gestion/Salaires Pierre/";

    public void readFile(String filename, CharSequence USELES)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
        {
            int nbLine = 0;
            int nbLineOk = 0;
            int nbLineNok = 0;
            String addComment = "";
            String salaireAnnee = "";
            String salaireMois = "";
            String salaireJour = "14";
            String salaireDate = "";
            listRBC = new ArrayList<RBCLineBean>();
            RBCLineBean lineBean = null;
            for (String line; (line = br.readLine()) != null;)
            {
                nbLine++;
                // System.out.println(line);

                if (nbLine == 1)
                {
                    // 1er ligne, c'est le libellé commun
                    addComment = line;

                    String releveDate = addComment.substring(addComment.length() - 7);

                    salaireAnnee = releveDate.substring(releveDate.length() - 4);
                    salaireMois = releveDate.substring(0, 2);

                    salaireDate = salaireJour + "/" + salaireMois + "/" + salaireAnnee;
                    System.out.println("Date=" + salaireDate);
                }
                else if (nbLine == 2)
                {
                    // 2eme ligne, en tête colonne
                }
                else
                {

                    // ligne salaire bloc1
                    if (lineBean != null)
                    {
                        listRBC.add(lineBean);
                    }
                    lineBean = new RBCLineBean();
                    lineBean.setDateOperation(salaireDate);

                    boolean montantSiNegatifAMettrePositif = false;
                    if (line.contains(REPORT_DEJA_EFFECTUES))
                    {
                        line = line.replace("( ", "").replace(" )", "");
                        montantSiNegatifAMettrePositif = true;
                    }

                    // virer les "(-)" en fin de ligne pour A reporter
                    if (line.contains(" (-)"))
                    {
                        System.out.println("oooh ?");
                        line = line.replace(" (-)", "");
                        montantSiNegatifAMettrePositif = true;
                    }

                    String description = line;
                    String montant = "0";
                    int firstSpace = line.indexOf(" ");
                    int lastSpace = line.lastIndexOf(" ");
                    if (lastSpace > 0)
                    {
                        montant = line.substring(lastSpace).trim();
                    }

                    if (StringUtils.isNumeric("" + line.charAt(0)))
                    {
                        // System.out.println("bloc1 [" + line + "]");
                        if ((firstSpace > 0) && (lastSpace > 0))
                        {
                            // System.out.println("firstSpace:" + firstSpace + ",lastSpace:" + lastSpace);
                            if (firstSpace != lastSpace)
                            {
                                description = line.substring(firstSpace + 1, lastSpace - 1);
                            }
                            else
                            {
                                description = line.substring(0, firstSpace);
                            }

                            int otherLast = description.lastIndexOf(" ");
                            if (otherLast > 0)
                            {
                                description = description.substring(0, otherLast);
                            }
                            otherLast = description.lastIndexOf(" ");
                            if (otherLast > 0)
                            {
                                description = description.substring(0, otherLast);
                            }
                        }
                    }
                    else
                    {
                        // System.out.println("blocN [" + line + "]");
                        description = line.substring(0, lastSpace);

                        String[] array =
                        { "Maladie Soins 2.80 %", "Maladie Soins NP 2.80 %", "Maladie Espèces 0.25 %",
                                "Caisse de Pension 8.00 %", "Caisse de Pension NP 8.00 %",
                                "Assurance dépendance 1.40 %", "Assurance dépendance NP 1.40 %",
                                "Impôt d'équi budg temp 0.50 %", "Impôt d'équi budg temp NP 0.50 %" };

                        boolean specialDebit = false;
                        for (String check : array)
                        {
                            if (description.contains(check))
                            {
                                specialDebit = true;
                                break;
                            }
                        }

                        if (specialDebit)
                        {
                            int otherLast = description.lastIndexOf(" ");
                            if (otherLast > 0)
                            {
                                description = description.substring(0, otherLast);
                            }
                            montant = "-" + montant;
                        }
                        else
                        {
                            if (description.equals("Impôt") || description.equals("Impôt NP"))
                            {
                                montant = "-" + montant;
                            }
                            else if (description.equals("CIS - CIM - CIP"))
                            {
                                if (montant.charAt(0) == '-')
                                {
                                    montant = montant.substring(1);
                                }
                                else
                                {
                                    montant = "-" + montant;
                                    // System.out.println("line " + line + " / montant=[" + montant + "]");
                                    // nbLineNok++;
                                }
                            }
                        }

                    }

                    // System.out.println(">desc:[" + description + "]");
                    // if (montant.charAt(0) == '-')
                    // {
                    // System.err.println(">montant:[" + montant + "]");
                    // }
                    // else
                    // {
                    // System.out.println(">montant:[" + montant + "]");
                    // }
                    montant = '"' + montant.replace(",", "") + '"';

                    String[] arrayForInformation =
                    { "Imposable", "Imposable NP", "Net", "Total net à virer", "Brut total", "Cotisations totales",
                            "Abattement", "Heures supplémentaires", "Suppléments DNF", "Frais de déplacement (FD)",
                            "Autres exemptions", "Total net", "Avantage prêt immobilier", "Exemption prêt immobilier" };

                    boolean forInformation = false;
                    for (String check : arrayForInformation)
                    {
                        if (description.equals(check))
                        {
                            forInformation = true;
                            break;
                        }
                    }

                    lineBean.setForInformation(forInformation);
                    lineBean.setDescription("RBC/" + description);

                    if (montantSiNegatifAMettrePositif)
                    {
                        if (montant.contains("-"))
                        {
                            montant = montant.replace("-", "");
                        }
                        else
                        {
                            montant = montant.replaceFirst("\"", "\"-");
                        }
                    }
                    lineBean.setMontant(montant);
                }

            }
            if (lineBean != null)
            {
                listRBC.add(lineBean);
            }

            System.out.println("Nb Ligne RBC = " + listRBC.size());
            for (RBCLineBean cbBean : listRBC)
            {
                String description = cbBean.getDescription();
                if (cbBean.isForInformation())
                {

                }
                else
                {
                    System.out.println(cbBean);
                }
                // nbLineNok++;
            }

            if (nbLineNok == 0)
            {

                File fout = new File(path + "Salaires-" + salaireAnnee + "/" + salaireAnnee + "-" + salaireMois
                        + "-Fiche-de-salaire-RBC.csv");

                System.out.println(fout.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(fout);

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "Cp1252"));

                // Float total = Float.valueOf(0);
                double total = 0.0;

                bw.write("Mode;Date;Details;Montant");
                bw.newLine();

                for (RBCLineBean cbBean : listRBC)
                {

                    if (!cbBean.isForInformation())
                    {
                        String stringMontant = cbBean.getMontant().replace("\"", "");

                        // Float montant = Float.parseFloat(stringMontant);
                        // DecimalFormat df = new DecimalFormat("0.00");
                        // df.setMaximumFractionDigits(2);

                        double montant = Double.parseDouble(stringMontant);
                        total += montant;

                        cbBean.setMontant(cbBean.getMontant().replace(".", ","));
                        bw.write(cbBean.toString());
                        bw.newLine();
                    }
                }
                bw.close();
                System.out.println("Total = " + new DecimalFormat("#.##").format(total));
            }
            else
            {
                System.err.println("NbErr = " + nbLineNok);
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

    /**
     * @return the listRBC
     */
    public List<RBCLineBean> getListRBC()
    {
        return listRBC;
    }

    /**
     * @param listRBC the listRBC to set
     */
    public void setListRBC(List<RBCLineBean> listRBC)
    {
        this.listRBC = listRBC;
    }
}
