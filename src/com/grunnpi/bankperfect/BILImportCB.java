package com.grunnpi.bankperfect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class BILImportCB
{

    private List<CBLineBean> listCB;
    private final String path = "D:/Documents/[Mes Documents]/Gestion/Banques/BIL/";

    public void readFile(String filename)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
        {
            int nbLine = 0;
            int nbLineOk = 0;
            int nbLineNok = 0;
            String addComment = "";
            listCB = new ArrayList<CBLineBean>();
            CBLineBean lineBean = null;
            for (String line; (line = br.readLine()) != null;)
            {
                nbLine++;
                // System.out.println(line);

                if (nbLine == 1)
                {
                    // 1er ligne, c'est le libellé commun
                    addComment = line;
                }
                else if (line.charAt(2) == '/')
                {
                    // c'est une date : nouveau bean. on sauve le -1
                    if (lineBean != null)
                    {
                        listCB.add(lineBean);
                    }
                    lineBean = new CBLineBean();

                    lineBean.setDateTraitement(line.substring(0, 10));
                    lineBean.setDateTransaction(line.substring(11, 21));
                    lineBean.setDescription(line.substring(22));
                }
                else
                {
                    // retour à la ligne pour détail en plus sur la description
                    if (lineBean != null)
                    {
                        lineBean.setDescription(lineBean.getDescription() + " " + line);
                    }
                }
            }
            listCB.add(lineBean);

            System.out.println("Nb Ligne CB = " + listCB.size());
            for (CBLineBean cbBean : listCB)
            {
                String description = cbBean.getDescription();
                int lastSpace = description.lastIndexOf(' ');
                if (lastSpace > 0)
                {
                    String desc = description.substring(0, lastSpace);
                    String number = description.substring(lastSpace + 1);
                    System.out.println("[" + desc + "][" + number + "]");

                    if (desc.charAt(desc.length() - 1) == '-')
                    {
                        desc = desc.substring(0, desc.length() - 2).trim();
                        number = '-' + number;
                    }
                    cbBean.setDescription(desc.replace(',', ' ') + "-" + addComment);
                    cbBean.setMontant('"' + number + '"');
                    nbLineOk++;
                }
                else
                {
                    System.out.println("NOT FOUND !");
                    nbLineNok++;
                }
            }
            if (nbLineNok == 0)
            {

                String releveDate = addComment.substring(addComment.length() - 10);
                System.out.println(releveDate);
                String year = releveDate.substring(releveDate.length() - 4);
                String month = releveDate.substring(3, 5);
                String day = releveDate.substring(0, 2);

                String cardType = addComment.replace("RELEVE ", "");
                int pos = cardType.indexOf(" ");
                if (pos > 0)
                {
                    cardType = cardType.substring(0, pos);
                }
                File fout = new File(path + year + "-" + month + "-" + day + "-" + cardType + ".csv");

                System.out.println(fout.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(fout);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                double total = 0.0;
                bw.write("Mode;Date;Autre;Details;Montant");
                bw.newLine();
                for (CBLineBean cbBean : listCB)
                {

                    String stringMontant = cbBean.getMontant().replace("\"", "").replace(".", "").replace(",", ".");
                    double montant = Double.parseDouble(stringMontant);
                    total += montant;

                    bw.write(cbBean.toString());
                    bw.newLine();
                }
                bw.close();
                System.out.println("Total = " + total);
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
     * @return the listCB
     */
    public List<CBLineBean> getListCB()
    {
        return listCB;
    }

    /**
     * @param listCB the listCB to set
     */
    public void setListCB(List<CBLineBean> listCB)
    {
        this.listCB = listCB;
    }
}
