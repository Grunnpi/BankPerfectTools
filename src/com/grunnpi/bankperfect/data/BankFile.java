package com.grunnpi.bankperfect.data;

import java.io.File;

public class BankFile
{
    private boolean toRename = false;
    private boolean toMoveToArchive = false;
    private String targetName;


    public boolean isToRename()
    {
        return toRename;
    }

    public void setToRename(boolean toRename)
    {
        this.toRename = toRename;
    }

    public boolean isToMoveToArchive()
    {
        return toMoveToArchive;
    }

    public void setToMoveToArchive(boolean toMoveToArchive)
    {
        this.toMoveToArchive = toMoveToArchive;
    }

    public String getTargetName()
    {
        return targetName;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    private File file;

}
