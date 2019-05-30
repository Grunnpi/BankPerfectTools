package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IStatementPreparator
{
    void setContext(final String ask, final String askParseAndDump, final String accountSignature,
                    final String directoryToFetch,
                    final File exclude, final File mapping, final String fileExtention, final boolean layoutStripper, final String archiveDir);

    String getAccountSignature();
    File getExclude();
    File getMapping();
    boolean isLayoutStripper();

    String getAsk();

    String getAskParseAndDump();

    List<Statement> prepare(BankFile bankFile, List<String> lines, Map<String, String> mapping,
            final String accountSignature);

    List<Statement> processFiles() throws IOException;

    void fetchFiles();

    List<BankFile> getListBankFiles();

    boolean hasFile();
}
