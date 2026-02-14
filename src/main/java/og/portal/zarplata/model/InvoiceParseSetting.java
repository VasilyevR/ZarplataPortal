package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "invoice_parse_setting")
public class InvoiceParseSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_row", nullable = false)
    private int startRow;

    @Column(name = "article_col", nullable = false)
    private int articleCol;

    @Column(name = "quantity_col", nullable = false)
    private int quantityCol;

    @Column(name = "supplier_article_col", nullable = false)
    private int supplierArticleCol;
}