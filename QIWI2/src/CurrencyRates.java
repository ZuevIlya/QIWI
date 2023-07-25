import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CurrencyRates {

    public static void main(String[] args) {
        String code = null; // Код валюты
        String date = null; // Дата

        // Проверка наличия аргументов
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--code=")) {
                    code = arg.substring(7);

                } else if (arg.startsWith("--date=")) {
                    date = arg.substring(7);

                }
            }
        } else { // Проверка на указание строки аргументов
            System.out.println("Не указаны аргументы. Правильное использование: currency_rates --code=USD --date=2022-10-08");
            System.exit(1);
        }
        if (code == null || date == null) { // Проверка на указание корректных аргументов
            System.out.println("Неверно указаны аргументы. Правильное использование: currency_rates --code=USD --date=2022-10-08");
            System.exit(1);
        }

        try { // Преобразование формата данных и проверка данных на корректность
            LocalDate dateFormat = LocalDate.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);
            date = dateFormat.format(formatter);
        } catch (Exception e) {
            System.out.println("Неверный формат данных: " + e.getMessage());
        }

        try { // Формирование URL для запроса к API ЦБ РФ
            String urlString = "https://www.cbr.ru/scripts/XML_daily.asp?date_req=" + date;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");


            // Проверка кода ответа сервера
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Чтение данных из потока

                // Charset.forName("CP1251") - решает проблему с кириллицей
                InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream(), Charset.forName("CP1251"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Поиск курса по указанному коду валюты
                String currency = findCurrencyByCode(response.toString(), code);
                if (currency != null) {
                    System.out.println(currency);
                } else {
                    System.out.println("Курс для указанной валюты не найден.");
                }
            } else {
                System.out.println("Ошибка при выполнении запроса. Код ответа сервера: " + responseCode);
            }
            connection.disconnect();

        } catch (IOException e) {
            System.out.println("Ошибка при выполнении запроса: " + e.getMessage());
        }
    }

    // Метод для поиска курса по указанному коду валюты
    public static String findCurrencyByCode(String xml, String code) {
        String startTag = "<CharCode>" + code + "</CharCode>";
        int startIndex = xml.indexOf(startTag);
        if (startIndex != -1) {
            int valueIndex = xml.indexOf("<Value>", startIndex);
            int endTagIndex = xml.indexOf("</Value>", valueIndex);

            String value = xml.substring(valueIndex + 7, endTagIndex);
            String nameStartTag = "<Name>";
            String nameEndTag = "</Name>";
            int nameIndex = xml.indexOf(nameStartTag, startIndex);

            int nameValueIndex = xml.indexOf(">", nameIndex);
            int nameEndTagIndex = xml.indexOf(nameEndTag, nameValueIndex);

            String name = xml.substring(nameValueIndex + 1, nameEndTagIndex);
            return code + " (" + name + "): " + value;
        }
        return null;
    }
}