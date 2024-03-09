# Console application

The aim of the project is to extract data from the specified address and transfer the records to a preferred location.
is sent to the database.

This project was developed with Java 8 and above, and Hibernate 5 and above was used for database operations.

`startup.sh` is used to compile and run the project.


Data in the fields (`currencyCode, unit, forexBuying, forexSelling, banknoteBuying, banknoteSelling, crossRate, informationRate`) are retrieved from https://www.tcmb.gov.tr/kurlar/today.xml and saved in 4 tables.

## Table Names and Columns:
exchange_rate
- id
- create_date
- currency_code
- unit
- forex_buying
- forex_selling

banknote_rate
- id
- create_date
- currency_code
- unit
- banknote_buying
- banknote_selling

cross_rate
- id
- create_date
- currency_code
- unit
- cross_rate

information_rate
- id
- create_date
- currency_code
- unit
- information_rate

Only once when xml changes
It must be processed, so the `recorded_file` table is created and keeps the last records.