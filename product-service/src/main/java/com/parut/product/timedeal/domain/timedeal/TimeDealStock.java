package com.parut.product.timedeal.domain.timedeal;

import jakarta.persistence.*;

@Entity
@Table(name = "p_time_deal_stocks")
public class TimeDealStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

}
