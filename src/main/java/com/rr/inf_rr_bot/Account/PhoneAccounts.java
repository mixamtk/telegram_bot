package com.rr.inf_rr_bot.Account;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.*;

@Data
@Configuration
@ConfigurationProperties(prefix = "phone")
public class PhoneAccounts {
    private Map<String, String> accounts = new HashMap<>();
    private static final List<PhoneAccount> infoAccount = new ArrayList<>();
    private static boolean flagUpdated = false;
    public List<PhoneAccount> getInfoAccount()
    {
        if (infoAccount.isEmpty()) {
            accounts.forEach((key, value) -> {
                PhoneAccount phoneAccount = new PhoneAccount();
                if (key.contains("_")) {
                    String[] tempStr = key.trim().split("_", 2);
                    phoneAccount.setId(tempStr[0]);
                    phoneAccount.setPhone(tempStr[1]);
                } else {
                    phoneAccount.setPhone(key);
                }
                phoneAccount.setPassword(value);
                infoAccount.add(phoneAccount);
            });
            infoAccount.sort(Comparator.comparing(PhoneAccount::getId));
        }
        flagUpdated = true;
        return new ArrayList<>(infoAccount);
    }
    public static List<PhoneAccount> getCopyInfoAccount (){
        return new ArrayList<>(infoAccount);
    }

    public static void resetFlagUpdated (){
        flagUpdated = false;
    }
    public static boolean isUpdate (){
        return flagUpdated;
    }

}
