package org.example;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.ProcessingInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        Configuration config = new Configuration();
        config.configure();

        try (SessionFactory sessionFactory =config.buildSessionFactory();
             Session session = sessionFactory.getCurrentSession()) {

            try {

                Connection.Response response = Jsoup.connect("https://www.tcmb.gov.tr/kurlar/today.xml").execute();

                String lastModifiedDate = response.headers().get("Last-Modified");

                String fileName = convertFileName(lastModifiedDate);
                session.beginTransaction();

                if (checkFileUpdate(fileName, session)) {
                    return;
                }


                RecordedFile recordedFiles = new RecordedFile();
                recordedFiles.setCreateDate(LocalDateTime.now());
                recordedFiles.setFileName(fileName);

                session.save(recordedFiles);

                Document doc = response.parse();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String currentDate = sdf.format(new Date());

                Elements currencies = doc.getElementsByTag("Currency");


                for (Element currency : currencies) {
                    String kod = currency.attr("Kod");
                    Element unitElement = currency.getElementsByTag("Unit").first();
                    String unit = unitElement != null ? unitElement.text() : null;

                    double forexBuyingValue = parseDouble(currency, "ForexBuying");
                    double forexSellingValue = parseDouble(currency, "ForexSelling");
                    double banknoteBuyingValue = parseDouble(currency, "BanknoteBuying");
                    double banknoteSellingValue = parseDouble(currency, "BanknoteSelling");
                    double crossRateValue = parseDouble(currency, "CrossRate");
                    double informationRateValue = parseDouble(currency, "InformationRate");

                    ExchangeRate exchangeRate = createExchangeRate(currentDate, kod, unit, forexBuyingValue, forexSellingValue);
                    BanknoteRate banknoteRate = createBanknoteRate(currentDate, kod, unit, banknoteBuyingValue, banknoteSellingValue);
                    CrossRate crossRate = createCrossRate(currentDate, kod, unit, crossRateValue);
                    InformationRate informationRate = createInformationRate(currentDate, kod, unit, informationRateValue);

                    session.save(exchangeRate);
                    session.save(banknoteRate);
                    session.save(crossRate);
                    session.save(informationRate);
                }

                session.getTransaction().commit();
                recordFile(session, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                session.close();
                sessionFactory.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void recordFile(Session session, String fileName) {
        try {
            List<ExchangeRate> exchangeRates = session.createQuery("FROM ExchangeRate", ExchangeRate.class).list();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = docBuilder.newDocument();

            org.w3c.dom.Element tarihDateElement = xmlDoc.createElement("Tarih_Date");
            tarihDateElement.setAttribute("Tarih", "04.03.2024");
            tarihDateElement.setAttribute("Date", "03/04/2024");
            tarihDateElement.setAttribute("Bulten_No", "2024/45");
            xmlDoc.appendChild(tarihDateElement);

            ProcessingInstruction style = xmlDoc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"isokur.xsl\"");
            xmlDoc.insertBefore(style, tarihDateElement);

            for (ExchangeRate exchangeRate : exchangeRates) {
                org.w3c.dom.Element currencyElement = xmlDoc.createElement("Currency");
                currencyElement.setAttribute("CurrencyCode", exchangeRate.getCurrencyCode());

                org.w3c.dom.Element unitElement = xmlDoc.createElement("Unit");
                unitElement.setTextContent(exchangeRate.getUnit());
                currencyElement.appendChild(unitElement);

                org.w3c.dom.Element forexBuyingElement = xmlDoc.createElement("ForexBuying");
                forexBuyingElement.setTextContent(String.valueOf(exchangeRate.getForexBuying()));
                currencyElement.appendChild(forexBuyingElement);

                org.w3c.dom.Element forexSellingElement = xmlDoc.createElement("ForexSelling");
                forexSellingElement.setTextContent(String.valueOf(exchangeRate.getForexSelling()));
                currencyElement.appendChild(forexSellingElement);

                tarihDateElement.appendChild(currencyElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(xmlDoc);
            FileWriter writer = new FileWriter(fileName);
            transformer.transform(source, new StreamResult(writer));

            System.out.println("XML dosyası oluşturuldu: Rates_20240220_123001.xml");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    private static double parseDouble(Element currency, String tagName) {
        Element element = currency.getElementsByTag(tagName).first();
        String text = element != null ? element.text() : "";
        return text.isEmpty() ? 0.0 : Double.parseDouble(text);
    }

    private static ExchangeRate createExchangeRate(String currentDate, String kod, String unit, double forexBuyingValue, double forexSellingValue) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setCreateDate(currentDate);
        exchangeRate.setCurrencyCode(kod);
        exchangeRate.setUnit(unit);
        exchangeRate.setForexBuying(forexBuyingValue);
        exchangeRate.setForexSelling(forexSellingValue);
        return exchangeRate;
    }

    private static BanknoteRate createBanknoteRate(String currentDate, String kod, String unit, double banknoteBuyingValue, double banknoteSellingValue) {
        BanknoteRate banknoteRate = new BanknoteRate();
        banknoteRate.setCreateDate(currentDate);
        banknoteRate.setCurrencyCode(kod);
        banknoteRate.setUnit(unit);
        banknoteRate.setBanknoteBuying(banknoteBuyingValue);
        banknoteRate.setBanknoteSelling(banknoteSellingValue);
        return banknoteRate;
    }

    private static CrossRate createCrossRate(String currentDate, String kod, String unit, double crossRateValue) {
        CrossRate crossRate = new CrossRate();
        crossRate.setCreateDate(currentDate);
        crossRate.setCurrencyCode(kod);
        crossRate.setUnit(unit);
        crossRate.setCrossRate(crossRateValue);
        return crossRate;
    }

    private static InformationRate createInformationRate(String currentDate, String kod, String unit, double informationRateValue) {
        InformationRate informationRate = new InformationRate();
        informationRate.setCreateDate(currentDate);
        informationRate.setCurrencyCode(kod);
        informationRate.setUnit(unit);
        informationRate.setInformationRate(informationRateValue);
        return informationRate;
    }

    private static String convertFileName(String lastModifiedDate) {
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        try {
            Date inputDate = inputDateFormat.parse(lastModifiedDate);
            String outputDateStr = outputDateFormat.format(inputDate);

            return "Rates_" + outputDateStr;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static boolean checkFileUpdate(String fileName, Session session) {
        Query query = session.createQuery("from RecordedFile WHERE fileName = :name");
        query.setParameter("name", fileName);

        List<RecordedFile> results = query.list();

        return !results.isEmpty();
    }

}