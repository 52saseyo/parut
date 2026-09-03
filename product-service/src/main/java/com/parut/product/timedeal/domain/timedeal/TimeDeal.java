package com.parut.product.timedeal.domain.timedeal;

import jakarta.persistence.*;

@Entity
@Table(name = "p_time_deals")
public class TimeDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;
}
