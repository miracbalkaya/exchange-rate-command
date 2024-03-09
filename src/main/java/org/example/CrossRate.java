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
@Table(name = "cross_rate")
public class CrossRate extends BaseEntity{


    @Column(name = "cross_rate")
    private double crossRate;

}

