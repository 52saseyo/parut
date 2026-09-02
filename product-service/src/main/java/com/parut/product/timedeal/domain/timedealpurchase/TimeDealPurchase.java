package com.parut.product.timedeal.domain.timedealpurchase;

import jakarta.persistence.*;

@Entity
@Table(name = "p_time_deal_purchases")
public class TimeDealPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;
}
