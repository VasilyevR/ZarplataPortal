package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "create_date")
    @JdbcTypeCode(SqlTypes.DATE)
    private LocalDate createDate;

    @Column(name = "number", length = 150)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String number;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "sum", precision = 15, scale = 2)
    @JdbcTypeCode(SqlTypes.DECIMAL)
    private Double sum;

    @Column(name = "arrival_date")
    @JdbcTypeCode(SqlTypes.DATE)
    private LocalDate arrivalDate;

    @Column(name = "shipment_date")
    @JdbcTypeCode(SqlTypes.DATE)
    private LocalDate shipmentDate;

    @Column(name = "discount")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short discount;

    @Column(name = "given_sum", precision = 15, scale = 2)
    @JdbcTypeCode(SqlTypes.DECIMAL)
    private Double givenSum;

    @Column(name = "purchase_sum", precision = 15, scale = 2)
    @JdbcTypeCode(SqlTypes.DECIMAL)
    private Double purchaseSum;

    @Column(name = "calculated_sum", precision = 15, scale = 2)
    @JdbcTypeCode(SqlTypes.DECIMAL)
    private Double calculatedSum;

    @Column(name = "documents_status", length = 50)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String documentsStatus;

    @Column(name = "given_date")
    @JdbcTypeCode(SqlTypes.DATE)
    private LocalDate givenDate;

    @Column(name = "notes", length = 100)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String notes;
}