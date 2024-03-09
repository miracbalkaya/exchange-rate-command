package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate extends BaseEntity {


    @Column(name = "forex_buying")
    private double forexBuying;

    @Column(name = "forex_selling")
    private double forexSelling;

}

