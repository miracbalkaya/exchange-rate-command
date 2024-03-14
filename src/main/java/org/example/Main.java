package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ProcessingInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Crawler Started");

        Configuration config = new Configuration();
        config.configure();

        try (SessionFactory sessionFactory = config.buildSessionFactory();
             Session session = sessionFactory.getCurrentSession()) {

            logger.info("Session created");
            try {

                Connection.Response response = Jsoup.connect("https://www.tcmb.gov.tr/kurlar/today.xml").execute();

                String lastModifiedDate = response.headers().get("Last-Modified");

                String fileName = HelperMethods.convertFileName(lastModifiedDate);
                session.beginTransaction();
                logger.info("Transaction Started");

                if (HelperMethods.checkFileUpdate(fileName, session)) {
                    logger.info("No changes found in the document. Last change document {} ", fileName);
                    return;
                }

                RecordedFile recordedFiles = new RecordedFile();
                recordedFiles.setCreateDate(LocalDateTime.now());
                recordedFiles.setFileName(fileName);
                logger.info("Will be recorded file name:  {} ", fileName);

                session.save(recordedFiles);
                logger.info("Document saved: {}", fileName);

                Document doc = response.parse();
                logger.info("Document parsed");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String currentDate = sdf.format(new Date());

                Elements currencies = doc.getElementsByTag("Currency");

                for (Element currency : currencies) {
                    String kod = currency.attr("Kod");
                    Element unitElement = currency.getElementsByTag("Unit").first();
                    String unit = unitElement != null ? unitElement.text() : null;

                    double forexBuyingValue = HelperMethods.parseDouble(currency, "ForexBuying");
                    double forexSellingValue = HelperMethods.parseDouble(currency, "ForexSelling");
                    double banknoteBuyingValue = HelperMethods.parseDouble(currency, "BanknoteBuying");
                    double banknoteSellingValue = HelperMethods.parseDouble(currency, "BanknoteSelling");
                    double crossRateValue = HelperMethods.parseDouble(currency, "CrossRate");
                    double informationRateValue = HelperMethods.parseDouble(currency, "InformationRate");

                    ExchangeRate exchangeRate = HelperMethods.createExchangeRate(currentDate, kod, unit, forexBuyingValue, forexSellingValue);
                    BanknoteRate banknoteRate = HelperMethods.createBanknoteRate(currentDate, kod, unit, banknoteBuyingValue, banknoteSellingValue);
                    CrossRate crossRate = HelperMethods.createCrossRate(currentDate, kod, unit, crossRateValue);
                    InformationRate informationRate = HelperMethods.createInformationRate(currentDate, kod, unit, informationRateValue);

                    session.save(exchangeRate);
                    logger.info("exchangeRate saved {}", exchangeRate);
                    session.save(banknoteRate);
                    logger.info("banknoteRate saved {}", banknoteRate);
                    session.save(crossRate);
                    logger.info("crossRate saved {}", crossRate);
                    session.save(informationRate);
                    logger.info("informationRate saved {}", informationRate);
                }

                session.getTransaction().commit();
                logger.info("Transaction committed");
                recordFile(fileName);
                session.close();
                sessionFactory.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            logger.error("Something went wrong.", e);
        }

    }

    private static void recordFile(String fileName) {
        Configuration config = new Configuration();
        config.configure();

        try (SessionFactory sessionFactory = config.buildSessionFactory();
             Session session = sessionFactory.getCurrentSession()) {

            session.beginTransaction();
            logger.info("Session created");
            List<ExchangeRate> exchangeRates = session.createQuery("select e FROM ExchangeRate as e", ExchangeRate.class).list();
            logger.info("Get All ExchangeRates from DB");

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

            try (FileOutputStream output =
                         new FileOutputStream(fileName + ".xml")) {
                writeXml(xmlDoc, output);
                logger.info("XML file created: " + "_32Bit/" + fileName + ".xml");
            } catch (IOException e) {
                logger.error("Something went wrong.", e);
            }

            session.close();
        } catch (Exception e) {
            logger.error("Something went wrong.", e);
        }
    }

    private static void writeXml(org.w3c.dom.Document doc,
                                 OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);

    }

}