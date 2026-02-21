package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "salary_column_mapping")
public class SalaryColumnMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "excel_col_index", nullable = false)
    private int excelColIndex;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    @Column(name = "use_excel_color", nullable = false)
    private boolean useExcelColor;

    @Column(name = "is_salary", nullable = false)
    private boolean salary;

    @Column(name = "is_currency", nullable = false)
    private boolean currency;
}
