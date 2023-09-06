package com.rr.inf_rr_bot.Beans;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@ConfigurationProperties(prefix = "chat")
public class ChatID {
    private Map<String, String> id =  new HashMap<>();

    @Value("${chat.admin}")
    private  Long idAdmin;

    private static String idAdminStr = "";
    private static final Map<String, String> ids = new HashMap<>();

    @PostConstruct
    private void getIds()
    {
        idAdminStr = String.valueOf(idAdmin);
        if (ids.isEmpty()) {
            ids.putAll(id);
        }
    }
    public static Map<String, String> getCopyID() {
        return ids;
    }

    public static String getIdAdminStr () {
        return idAdminStr;
    }

}
