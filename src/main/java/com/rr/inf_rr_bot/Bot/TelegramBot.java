package com.rr.inf_rr_bot.Bot;

import com.rr.inf_rr_bot.Account.PhoneAccount;
import com.rr.inf_rr_bot.Account.PhoneAccounts;
import com.rr.inf_rr_bot.Beans.ChatID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText){
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/chatID":
                    sendMessage(chatId, String.valueOf(chatId));
                    break;
                case "/updateAll":
                    PhoneAccounts.resetFlagUpdated();
                    sendMessage(chatId, "All records will be updated in the next update");
                    break;
                case "/b":
                    Map<String,String> chatIDs = ChatID.getCopyID();
                    String chatIDStr = String.valueOf(chatId);
                    String id = chatIDs.get(chatIDStr);
                    StringBuilder response = new StringBuilder();
                    long adminID = Long.parseLong(ChatID.getIdAdminStr());
                    if (chatIDs.containsKey(chatIDStr)) {
                        for (PhoneAccount phoneAccount : PhoneAccounts.getCopyInfoAccount()) {
                            if (phoneAccount.getId().equals(id)) response.append(phoneAccount);
                        }
                        sendMessage(chatId, response.toString());
                    }
                    if (ChatID.getIdAdminStr().equals(chatIDStr)) {
                        for (PhoneAccount phoneAccount : PhoneAccounts.getCopyInfoAccount()) {
                            response.append(phoneAccount);
                        }
                        sendMessage(adminID, response.toString());
                    }
                default:

            }
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + "!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }
}