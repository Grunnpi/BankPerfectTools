package com.grunnpi.bankperfect.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ConsoleHelper
{
    public static boolean readConsole(final String question, final String response, final String positive)
            throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(question + " [" + response + "] : ");
        String s = br.readLine();
        return s.equalsIgnoreCase(positive);
    }

    public static String readConsoleMultipleChoice(final String question, final List<String> responsesList)
            throws IOException
    {
        String[] responses = new String[responsesList.size()];
        responses = responsesList.toArray(responses);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(question);
        for (String r : responses)
        {
            System.out.println(" " + r);
        }
        return br.readLine();
    }
}
