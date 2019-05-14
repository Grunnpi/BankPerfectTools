package com.grunnpi.bankperfect;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbstractParser
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractParser.class);

    protected AccountID getAccount(final String accountSignature, final String cartType ) {

        AccountID accountID = new AccountID();

        boolean foundIt = false;
        String[] accountPerCardIdList = accountSignature.split(";");
        for ( String accountPerCardId : accountPerCardIdList ) {
            String[] accountIdKeySplit = accountPerCardId.split("#");
            if ( accountIdKeySplit[0].equals(cartType)) {
                String[] accountIdString = accountIdKeySplit[1].split(",");
                accountID.setBank(accountIdString[0]);
                accountID.setBranch(accountIdString[1]);
                accountID.setAccount(accountIdString[2]);
                foundIt = true;
                break;
            }
        }

        if ( !foundIt) {
            throw new RuntimeException("Cannot find signature for [" + cartType + "] in [" + accountSignature + "]");
        }

        return accountID;
    }

    protected static boolean areEqualByThreeDecimalPlaces(double a, double b)
    {
        DecimalFormat df = new DecimalFormat("####.##;-####.##");
        df.setRoundingMode(RoundingMode.UP);
        String as  = df.format(a);
        String bs  = df.format(b);
//        LOG.info("[{}].equals[{}]={} <while input value are {} and {}>",as,bs,as.equals(bs),a,b);
        return as.equals(bs);
    }
}
