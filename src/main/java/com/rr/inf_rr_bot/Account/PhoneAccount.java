package com.rr.inf_rr_bot.Account;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class PhoneAccount {
    private String id = "";
    private String phone = "";
    private String password = "";
    private int smsBonus = -1;
    private double balance;
    private LocalDateTime dateTimeUpdate;

    public PhoneAccount() {
    }

    public PhoneAccount(PhoneAccount phoneAccount) {
        this.id = phoneAccount.getId();
        this.phone = phoneAccount.getPhone();
        this.password = phoneAccount.getPassword();
        this.smsBonus = phoneAccount.getSmsBonus();
        this.balance = phoneAccount.getBalance();
        this.dateTimeUpdate = phoneAccount.getDateTimeUpdate();
    }



    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return ("Phone number: +375" + phone +  " " + id + "\n" +
                "SMS bonus = " + smsBonus + "\n" +
                "Balance = " + balance + "\n" +
                "Last Update = " + dateTimeUpdate.format(formatter) +
                "\n\n");
    }
}
