package com.grunnpi.bankperfect;

import java.text.ParseException;

public class Bankperfect
{

    public enum fileType
    {
        cb, rbc, doenst, old
    };

    public static void main(String[] args) throws ParseException
    {
        fileType myFileType = fileType.rbc;

        switch (myFileType)
        {
        case cb:
            BILImportCB bilImportCB = new BILImportCB();
            String filenameCB = "./resource/CB-BIL.txt";
            bilImportCB.readFile(filenameCB);
            break;
        case old:
            RBCImportSalaireOld rbcImportSalaireOld = new RBCImportSalaireOld();
            String filenameRBCold = "./resource/RBC-SalaireOld.txt";
            rbcImportSalaireOld.readFile(filenameRBCold, ".");
            break;
        case rbc:
            RBCImportSalaire rbcImportSalaire = new RBCImportSalaire();
            String filenameRBC = "./resource/RBC-Salaire.txt";
            rbcImportSalaire.readFile(filenameRBC, ".");
            break;
        case doenst:
            DoenstImport doenstImport = new DoenstImport();
            String filenameDoenst = "./resource/Doenst_Beethoven.txt";
            doenstImport.readFile(filenameDoenst);
            break;
        }
    }

}
