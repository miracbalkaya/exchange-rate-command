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
@Table(name = "banknote_rate")
public class BanknoteRate extends BaseEntity{

    @Column(name = "banknote_buying")
    private double banknoteBuying;

    @Column(name = "banknote_selling")
    private double banknoteSelling;


}

