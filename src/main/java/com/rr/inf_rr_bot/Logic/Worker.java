package com.rr.inf_rr_bot.Logic;

import com.rr.inf_rr_bot.Account.PhoneAccount;
import com.rr.inf_rr_bot.Account.PhoneAccounts;
import com.rr.inf_rr_bot.Beans.ChatID;
import com.rr.inf_rr_bot.Bot.TelegramBot;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@Data
public class Worker {
    @Value("${work.urlHome}")
    private String urlStart;
    @Value("${work.urlStatus}")
    private String urlStatus;
    @Value("${work.urlLogOff}")
    private String urlSLogOff;
    private final PhoneAccounts accounts;
    private final TelegramBot bot;

    @Autowired
    public Worker(PhoneAccounts accounts,TelegramBot bot) {
        this.accounts = accounts;
        this.bot = bot;
    }

    private CookieManager cookieManager = new CookieManager();

    public void process() {
        try {
            // устанавливаем дорверие для всех сертификатов
            setTrustAllCert();
            // устанавливаем менеджер cookie
            CookieHandler.setDefault(cookieManager);
            // Устанавливаем политику обработки куков
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            // Отправляем GET-запрос для получения кук
            sendGetRequest(urlStart);
            List<PhoneAccount> phoneAccounts;
            if (!PhoneAccounts.isUpdate()) {
                phoneAccounts = accounts.getInfoAccount();
            } else {
                phoneAccounts = PhoneAccounts.getCopyInfoAccount().stream()
                                                                    .filter(t -> t.getSmsBonus() < 500)
                                                                    .collect(Collectors.toList());
                if (phoneAccounts.get(0).getDateTimeUpdate().getDayOfMonth() != Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                    PhoneAccounts.resetFlagUpdated();
                }
            }
            for (PhoneAccount phoneAccount:phoneAccounts) {
                // Авторизуемся
                authenticate(urlStart, phoneAccount);
                PhoneAccount oldPhoneAccount = new PhoneAccount(phoneAccount);
                // Переходим на нужный адрес, парсим ответ и заполняем объект
                sendGetParseRequest(urlStatus,phoneAccount);
                System.out.println(phoneAccount);
                // отправка сообщения, если баланс sms изменился с пошлого раза  и меньше 100
                if (oldPhoneAccount.getSmsBonus() != phoneAccount.getSmsBonus()){
                    if (phoneAccount.getSmsBonus() < 100) {
                        // отправка сообещния администратару
                        sendSMSLimit(ChatID.getIdAdminStr(), phoneAccount);
                        //отправка сообщения всем из области
                        for (Map.Entry<String, String> entry: ChatID.getCopyID().entrySet()) {
                            if (entry.getValue().equals(phoneAccount.getId())) {
                                sendSMSLimit(entry.getKey(), phoneAccount);
                            }
                        }
                    }
                }
                // Завершаем сеанс
                sendGetRequest(urlSLogOff);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | KeyManagementException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTrustAllCert() throws NoSuchAlgorithmException, KeyManagementException {
        //  Создаем CookieManager и устанавливаем его как дефолтный для приложения
        //  Создаем TrustManager, который принимает все сертификаты
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};
        // Создаем SSLContext с нашим TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        // Настраиваем HttpsURLConnection для использования нашего SSLContext
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    private void sendSMSLimit(String idChat, PhoneAccount phoneAccount) throws TelegramApiException {
        bot.execute(new SendMessage(idChat, "Warning! " + phoneAccount.getId() + " +375" +
                phoneAccount.getPhone() + " - " +
                phoneAccount.getSmsBonus() + " sms left"));
    }

    private void sendGetParseRequest(String urlStr, PhoneAccount phoneAccount) throws IOException, URISyntaxException {
        // Создаем URL объект для указания адреса веб-страницы
        URL url = new URL(urlStr);
        // Открываем соединение с URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Устанавливаем метод запроса (GET по умолчанию)
        connection.setRequestMethod("GET");
        // Получаем куки из CookieManager
        // Получаем ответный код
        int responseCode = connection.getResponseCode();
        // Проверяем успешность запроса
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Получаем заголовки ответа
            // Создаем BufferedReader для чтения содержимого ответа
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            // Читаем содержимое ответа построчно
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // Выводим содержимое ответа
            //System.out.println("Response GET:\n" + response);
            parseAndUpdatePhoneAccount(phoneAccount, response);
        } else {
            System.out.println("Error: " + responseCode);
        }

        // Закрываем соединение
        connection.disconnect();
    }

    private static void parseAndUpdatePhoneAccount(PhoneAccount phoneAccount, StringBuilder response) {
        String responseStr = response.toString();
        String regexSms = "(\\d+)\\sшт";
        Pattern patternSms = Pattern.compile(regexSms);
        Matcher matcher = patternSms.matcher(responseStr);
        if (matcher.find()) {
            phoneAccount.setSmsBonus(Integer.parseInt(matcher.group(1)));
        } else {
            phoneAccount.setSmsBonus(0);
        }
        String regexBalance = "(\\d{1,2},\\d{1,2})\\sруб";
        Pattern patternBalance = Pattern.compile(regexBalance);
        matcher = patternBalance.matcher(responseStr);
        if (matcher.find())
            phoneAccount.setBalance(Double.parseDouble(matcher.group(1).replace(",", ".")));
        phoneAccount.setDateTimeUpdate(LocalDateTime.now());
    }

    private void sendGetRequest(String urlStr) throws IOException, URISyntaxException {
        // Создаем URL объект для указания адреса веб-страницы
        URL url = new URL(urlStr);
        // Открываем соединение с URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Устанавливаем метод запроса (GET по умолчанию)
        connection.setRequestMethod("GET");
        // Получаем куки из CookieManager
        // Получаем ответный код
        int responseCode = connection.getResponseCode();
        // Проверяем успешность запроса
        if (responseCode == HttpURLConnection.HTTP_OK) {
            //System.out.println("Sucsess GET:");
        } else {
            System.out.println("Error: " + responseCode);
        }
        // Закрываем соединение
        connection.disconnect();
    }

    private void authenticate(String urlStr,PhoneAccount phoneAccount) throws IOException, URISyntaxException {
        // Создаем URL объект для указания адреса веб-страницы
        URL url = new URL(urlStr);
        // Открываем соединение с URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Устанавливаем метод запроса (POST)
        connection.setRequestMethod("POST");
        // Получаем куки из CookieManager
        String cookies = cookieManager.getCookieStore().getCookies().toString();
        // Устанавливаем заголовок Cookie с полученными куками
        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies);
        }
        // Включаем режим вывода данных
        connection.setDoOutput(true);
        // Устанавливаем параметры payload с логином и паролем
        String payload = "username=" + phoneAccount.getPhone() + "&password=" + phoneAccount.getPassword();
        // Получаем OutputStream для записи данных
        OutputStream outputStream = connection.getOutputStream();
        // Записываем данные в поток в виде байтового массива
        byte[] payloadBytes = payload.getBytes();
        outputStream.write(payloadBytes);
        outputStream.flush();
        outputStream.close();
        // Получаем ответный код
        int responseCode = connection.getResponseCode();
        // Проверяем успешность запроса
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Создаем BufferedReader для чтения содержимого ответа
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            // Читаем содержимое ответа построчно
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // Выводим содержимое ответа
            //System.out.println("Response POST:\n" + response.toString());
        } else {
            System.out.println("Error: " + responseCode);
        }
        // Закрываем соединение
        connection.disconnect();
    }

}
