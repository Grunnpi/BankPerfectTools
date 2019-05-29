package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.Statement;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface IStatementPreparator
{
    void setContext(final String accountSignature, final String directoryToFetch,
            final File exclude, final File mapping, final String fileExtention,final boolean layoutStripper);

    String getAccountSignature();
    File getDirectoryToFetch();
    File getExclude();
    File getMapping();
    String[] getFileExtention();
    boolean isLayoutStripper();

    List<Statement> prepare(List<String> lines,Map<String,String> mapping, final String accountSignature);

    void fetchFiles();

    boolean hasFile();
}
