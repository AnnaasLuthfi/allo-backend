package com.test.alo.split_bill_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "bill_group_id", "name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_group_id", nullable = false)
    private BillGroup billGroup;
}
