package org.example;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HelperMethods {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static double parseDouble(Element currency, String tagName) {
        Element element = currency.getElementsByTag(tagName).first();
        String text = element != null ? element.text() : "";
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    public static ExchangeRate createExchangeRate(String currentDate, String kod, String unit, double forexBuyingValue, double forexSellingValue) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCreateDate(currentDate);
        exchangeRate.setCurrencyCode(kod);
        exchangeRate.setUnit(unit);
        exchangeRate.setForexBuying(forexBuyingValue);
        exchangeRate.setForexSelling(forexSellingValue);
        return exchangeRate;
    }

    public static BanknoteRate createBanknoteRate(String currentDate, String kod, String unit, double banknoteBuyingValue, double banknoteSellingValue) {
        BanknoteRate banknoteRate = new BanknoteRate();
        banknoteRate.setCreateDate(currentDate);
        banknoteRate.setCurrencyCode(kod);
        banknoteRate.setUnit(unit);
        banknoteRate.setBanknoteBuying(banknoteBuyingValue);
        banknoteRate.setBanknoteSelling(banknoteSellingValue);
        return banknoteRate;
    }

    public static CrossRate createCrossRate(String currentDate, String kod, String unit, double crossRateValue) {
        CrossRate crossRate = new CrossRate();
        crossRate.setCreateDate(currentDate);
        crossRate.setCurrencyCode(kod);
        crossRate.setUnit(unit);
        crossRate.setCrossRate(crossRateValue);
        return crossRate;
    }

    public static InformationRate createInformationRate(String currentDate, String kod, String unit, double informationRateValue) {
        InformationRate informationRate = new InformationRate();
        informationRate.setCreateDate(currentDate);
        informationRate.setCurrencyCode(kod);
        informationRate.setUnit(unit);
        informationRate.setInformationRate(informationRateValue);
        return informationRate;
    }

    public static String convertFileName(String lastModifiedDate) {
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        try {
            Date inputDate = inputDateFormat.parse(lastModifiedDate);
            String outputDateStr = outputDateFormat.format(inputDate);

            return "Rates_" + outputDateStr;
        } catch (ParseException e) {
            logger.error("Something went wrong.", e);
        }
        return "";
    }

    public static boolean checkFileUpdate(String fileName, Session session) {
        Query query = session.createQuery("from RecordedFile WHERE fileName = :name");
        query.setParameter("name", fileName);

        List<RecordedFile> results = query.list();

        return !results.isEmpty();
    }


}
