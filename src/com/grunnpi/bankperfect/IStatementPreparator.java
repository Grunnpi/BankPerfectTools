package com.grunnpi.bankperfect;

import java.util.List;
import java.util.Map;

public interface IStatementPreparator
{

    List<Statement> prepare(List<String> lines,Map<String,String> mapping);
}
