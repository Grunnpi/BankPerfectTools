package com.grunnpi.bankperfect;

import java.util.List;

public interface IStatementPreparator
{

    List<Statement> prepare(List<String> lines);
}
