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
@Table(name = "information_rate")
public class InformationRate extends BaseEntity {

    @Column(name = "information_rate")
    private double informationRate;


}

