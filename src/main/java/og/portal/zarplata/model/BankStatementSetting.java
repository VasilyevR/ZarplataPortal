package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "bank_statement_setting")
public class BankStatementSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_name", nullable = false, unique = true)
    private String bankName;

    @Column(name = "date_format", nullable = false)
    private String dateFormat;

    @Column(name = "start_row", nullable = false)
    private int startRow;

    @Column(name = "amount_col_index", nullable = false)
    private int amountColIndex;

    @Column(name = "date_col_index", nullable = false)
    private int dateColIndex;

    @Column(name = "client_name_col_index", nullable = false)
    private int clientNameColIndex;
    
    @Column(name = "subject_col_index", nullable = false)
    private int subjectColIndex;
}
