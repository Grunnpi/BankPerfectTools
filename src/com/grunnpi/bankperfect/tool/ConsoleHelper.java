package com.grunnpi.bankperfect.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public static String readConsoleMultipleChoice(final String question, final String[] response) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(question);
        for (String r : response)
        {
            System.out.println(" " + r);
        }
        return br.readLine();
    }
}
